package org.cardanofoundation.job.service.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.explorer.common.entity.enumeration.DataCheckpointType;
import org.cardanofoundation.explorer.common.entity.explorer.DataCheckpoint;
import org.cardanofoundation.explorer.common.entity.ledgersync.Block;
import org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount;
import org.cardanofoundation.job.repository.explorer.DataCheckpointRepository;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.TokenTxCountRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.service.TokenTxCountService;
import org.cardanofoundation.job.util.BatchUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenTxCountServiceImpl implements TokenTxCountService {

  @Autowired @Lazy TokenTxCountServiceImpl selfProxyService;

  private final CardanoConverters cardanoConverters;
  private final BlockRepository blockRepository;
  private final AddressTxAmountRepository addressTxAmountRepository;
  private final DataCheckpointRepository dataCheckpointRepository;
  private final TokenTxCountRepository tokenTxCountRepository;

  private static final int DEFAULT_BATCH_SIZE = 30000;

  @Value("${application.network}")
  private String network;

  @Value("${jobs.token-info.num-slot-interval}")
  private Integer NUM_SLOT_INTERVAL;

  @Override
  public void updateTokenTxCount() {

    Long currentSlot = cardanoConverters.time().toSlot(LocalDateTime.now(ZoneOffset.UTC));

    Optional<Block> latestBlockFromLs = blockRepository.findLatestBlock();

    // Get the max slot from LS_AGG
    Long maxSlotFromLSAgg = addressTxAmountRepository.getMaxSlotNoFromCursor();

    if (latestBlockFromLs.isEmpty() || maxSlotFromLSAgg == null) {
      log.error("No latest block found in LS or no max slot found in LS_AGG");
      return;
    }

    // Get the max slot from LS
    Long maxSlotFromLS = latestBlockFromLs.get().getSlotNo();

    // Get the latest processed checkpoint
    DataCheckpoint latestProcessedCheckpoint =
        dataCheckpointRepository
            .findFirstByType(DataCheckpointType.TOKEN_TX_COUNT)
            .orElse(
                DataCheckpoint.builder()
                    .slotNo(0L)
                    .type(DataCheckpointType.TOKEN_TX_COUNT)
                    .build());

    Long startSlot =
        cardanoConverters.time().toSlot(cardanoConverters.genesisConfig().getStartTime());

    Long latestProcessedSlot = latestProcessedCheckpoint.getSlotNo();
    Long endSlot = latestProcessedSlot + NUM_SLOT_INTERVAL;

    // Get the min of the max slot from LS, LS_AGG and the new checkpoint. That ensure the
    // consistency data from multiple sources
    Long maxSafeProcessableSlot =
        Stream.of(maxSlotFromLS, maxSlotFromLSAgg, endSlot).min(Long::compareTo).get();

    LocalDateTime maxSafeProcessableTime =
        cardanoConverters.slot().slotToTime(maxSafeProcessableSlot);

    Long previousValuesSafeProcessableSlot = maxSafeProcessableSlot;

    while (ChronoUnit.MINUTES.between(maxSafeProcessableTime, LocalDateTime.now(ZoneOffset.UTC))
        > 60L) {
      // As long as the upper bound of our time interval is older than 1h, we can safely while loop
      // and
      // not care about rollbacks.
      DataCheckpoint checkpoint =
          selfProxyService.processTokenInSlotRange(latestProcessedSlot, endSlot);
      latestProcessedSlot = checkpoint.getSlotNo();
      endSlot = latestProcessedSlot + NUM_SLOT_INTERVAL;

      maxSafeProcessableSlot =
          Stream.of(endSlot, maxSlotFromLS, maxSlotFromLSAgg).min(Long::compareTo).get();

      if (previousValuesSafeProcessableSlot.equals(maxSafeProcessableSlot)) {
        log.error("No progress in processing token tx count. Breaking the loop");
      } else {
        previousValuesSafeProcessableSlot = maxSafeProcessableSlot;
      }
      maxSafeProcessableTime = cardanoConverters.slot().slotToTime(maxSafeProcessableSlot);
    }
    log.info("The token tx count job switched to incremental mode");
    selfProxyService.processTokenTxCountFromSafetySlotToTip(latestProcessedSlot, currentSlot);
    log.info(
        "Token tx count scheduler finished processing. The data had processed up to slot {}",
        currentSlot);
  }

  @Transactional
  protected DataCheckpoint processTokenInSlotRange(Long startSlot, Long endSlot) {
    long startTime = System.currentTimeMillis();
    log.info("Processing token tx count for slots {} to {}", startSlot, endSlot);

    DataCheckpoint newCheckpoint =
        DataCheckpoint.builder()
            .slotNo(endSlot)
            .type(DataCheckpointType.TOKEN_TX_COUNT)
            .updateTime(Timestamp.from(Instant.now()))
            .build();

    List<TokenTxCount> tokenTxCounts =
        addressTxAmountRepository.getTotalTxCountByUnitInSlotRange(startSlot, endSlot);

    if (CollectionUtils.isEmpty(tokenTxCounts)) {
      log.info("No units found for slots {} to {}", startSlot, endSlot);
      return newCheckpoint;
    }

    BatchUtils.doBatching(
        DEFAULT_BATCH_SIZE,
        tokenTxCounts,
        list -> buildTokenTxCountListInRollbackCaseMightNotOccur(list, endSlot));

    dataCheckpointRepository.save(newCheckpoint);
    log.info(
        "Processing token tx count for slots {} to {} took {} ms",
        startSlot,
        endSlot,
        System.currentTimeMillis() - startTime);
    return newCheckpoint;
  }

  @Transactional
  protected void processTokenTxCountFromSafetySlotToTip(Long latestProcessedSlot, Long tip) {
    List<TokenTxCount> tokenTxCounts =
        addressTxAmountRepository.getTotalTxCountByUnitInSlotRange(latestProcessedSlot, tip);

    if (CollectionUtils.isEmpty(tokenTxCounts)) {
      log.info("No units found for slots {} to {}", latestProcessedSlot, tip);
      return;
    }
    BatchUtils.doBatching(
        DEFAULT_BATCH_SIZE,
        tokenTxCounts,
        list -> buildTokenTxCountListInRollbackCaseMightOccur(list, tip));
  }
  // The function is used in cases where data is saved to the database and a rollback might not
  // occur.
  // Init Mode
  // Note that:
  // - All values had been computed and the isCalculatedInIncrementalMode fields are set to FALSE
  // - The updated values and previous values always can be TRUSTED
  void buildTokenTxCountListInRollbackCaseMightNotOccur(
      List<TokenTxCount> tokenTxCounts, Long endSlot) {
    List<String> units = tokenTxCounts.stream().map(TokenTxCount::getUnit).toList();
    List<TokenTxCount> existingTokenTxCounts = tokenTxCountRepository.findAllByUnitIn(units);
    Map<String, TokenTxCount> existingTokenTxCountMap =
        existingTokenTxCounts.stream()
            .collect(Collectors.toMap(TokenTxCount::getUnit, Function.identity()));
    tokenTxCounts.parallelStream()
        .forEach(
            tokenTxCount -> {
              if (existingTokenTxCountMap.containsKey(tokenTxCount.getUnit())) {
                TokenTxCount existingTokenTxCount =
                    existingTokenTxCountMap.get(tokenTxCount.getUnit());
                if (existingTokenTxCount.getIsCalculatedInIncrementalMode()
                    && Objects.isNull(existingTokenTxCount.getPreviousSlot())) {
                  tokenTxCount.setPreviousSlot(endSlot);
                  tokenTxCount.setPreviousTxCount(tokenTxCount.getTxCount());
                } else if (existingTokenTxCount.getIsCalculatedInIncrementalMode()
                    && Objects.nonNull(existingTokenTxCount.getPreviousSlot())) {
                  tokenTxCount.setTxCount(
                      tokenTxCount.getTxCount() + existingTokenTxCount.getPreviousTxCount());
                  tokenTxCount.setPreviousTxCount(existingTokenTxCount.getPreviousTxCount());
                  tokenTxCount.setPreviousSlot(existingTokenTxCount.getPreviousSlot());
                } else if (!existingTokenTxCount.getIsCalculatedInIncrementalMode()
                    && Objects.nonNull(existingTokenTxCount.getPreviousSlot())) {
                  tokenTxCount.setPreviousSlot(existingTokenTxCount.getUpdatedSlot());
                  tokenTxCount.setPreviousTxCount(existingTokenTxCount.getTxCount());
                  tokenTxCount.setTxCount(
                      tokenTxCount.getTxCount() + existingTokenTxCount.getTxCount());
                }

              } else {
                tokenTxCount.setPreviousSlot(endSlot);
                tokenTxCount.setPreviousTxCount(tokenTxCount.getTxCount());
              }
              tokenTxCount.setUpdatedSlot(endSlot);
              tokenTxCount.setIsCalculatedInIncrementalMode(false);
            });
    tokenTxCountRepository.saveAll(tokenTxCounts);
  }

  // The function is used in cases where data is saved to the database and a rollback might occur.
  // Incremental Mode
  // Note that:
  // - After updating the token info, the isCalculatedInIncrementalMode fields are set to TRUE
  // - the updated values can be UNTRUSTED, but the previous values always can be TRUSTED
  void buildTokenTxCountListInRollbackCaseMightOccur(
      List<TokenTxCount> tokenTxCounts, Long endSlot) {
    List<String> units = tokenTxCounts.stream().map(TokenTxCount::getUnit).toList();
    List<TokenTxCount> existingTokenTxCounts = tokenTxCountRepository.findAllByUnitIn(units);
    Map<String, TokenTxCount> existingTokenTxCountMap =
        existingTokenTxCounts.stream()
            .collect(Collectors.toMap(TokenTxCount::getUnit, Function.identity()));
    tokenTxCounts.parallelStream()
        .forEach(
            tokenTxCount -> {
              if (existingTokenTxCountMap.containsKey(tokenTxCount.getUnit())) {
                TokenTxCount existingTokenTxCount =
                    existingTokenTxCountMap.get(tokenTxCount.getUnit());
                if (existingTokenTxCount.getIsCalculatedInIncrementalMode()
                    && Objects.nonNull(existingTokenTxCount.getPreviousSlot())) {
                  tokenTxCount.setTxCount(
                      tokenTxCount.getTxCount() + existingTokenTxCount.getPreviousTxCount());
                  tokenTxCount.setPreviousSlot(existingTokenTxCount.getPreviousSlot());
                  tokenTxCount.setPreviousTxCount(existingTokenTxCount.getPreviousTxCount());
                } else if (!existingTokenTxCount.getIsCalculatedInIncrementalMode()) {
                  tokenTxCount.setPreviousSlot(existingTokenTxCount.getUpdatedSlot());
                  tokenTxCount.setPreviousTxCount(existingTokenTxCount.getTxCount());
                  tokenTxCount.setTxCount(
                      tokenTxCount.getTxCount() + existingTokenTxCount.getTxCount());
                }
              }
              tokenTxCount.setUpdatedSlot(endSlot);
              tokenTxCount.setIsCalculatedInIncrementalMode(true);
            });
    tokenTxCountRepository.saveAll(tokenTxCounts);
  }
}
