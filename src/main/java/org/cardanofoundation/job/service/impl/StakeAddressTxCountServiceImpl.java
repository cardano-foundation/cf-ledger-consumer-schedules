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
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.explorer.common.entity.enumeration.DataCheckpointType;
import org.cardanofoundation.explorer.common.entity.explorer.DataCheckpoint;
import org.cardanofoundation.explorer.common.entity.ledgersync.Block;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.StakeAddressTxCount;
import org.cardanofoundation.job.repository.explorer.DataCheckpointRepository;
import org.cardanofoundation.job.repository.explorer.jooq.JOOQDataCheckpointRepository;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxCountRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.StakeAddressTxCountRepository;
import org.cardanofoundation.job.service.StakeAddressTxCountService;
import org.cardanofoundation.job.util.BatchUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class StakeAddressTxCountServiceImpl implements StakeAddressTxCountService {
  @Autowired @Lazy StakeAddressTxCountServiceImpl selfProxyService;
  private final JOOQDataCheckpointRepository jooqDataCheckpointRepository;
  private final CardanoConverters cardanoConverters;
  private final BlockRepository blockRepository;
  private final AddressTxAmountRepository addressTxAmountRepository;
  private final DataCheckpointRepository dataCheckpointRepository;
  private final StakeAddressTxCountRepository stakeAddressTxCountRepository;
  private static final Integer DEFAULT_BATCH_SIZE = 500;
  private final String JOB_NAME = "StakeAddressesTxCount";

  @Value("${jobs.stake-address-tx-count.num-slot-interval}")
  private Integer NUM_SLOT_INTERVAL;

  @Autowired private AddressTxCountRepository addressTxCountRepository;

  @Override
  public void updateStakeAddressTxCount() {
    Long currentSlot = cardanoConverters.time().toSlot(LocalDateTime.now(ZoneOffset.UTC));

    Optional<Block> latestBlockFromLs = blockRepository.findLatestBlock();

    // Get the max slot from LS_AGG
    Long maxSlotFromLSAgg = addressTxAmountRepository.getMaxSlotNoFromCursor();

    if (latestBlockFromLs.isEmpty() || maxSlotFromLSAgg == null) {
      log.error(
          "No latest block found in LS or no max slot found in LS_AGG --- Job: [{}] ---", JOB_NAME);
      return;
    }
    // Get the max slot from LS
    Long maxSlotFromLS = latestBlockFromLs.get().getSlotNo();

    Long startSlot =
        cardanoConverters.time().toSlot(cardanoConverters.genesisConfig().getStartTime());
    // Get the latest processed checkpoint
    DataCheckpoint latestProcessedCheckpoint =
        dataCheckpointRepository
            .findFirstByType(DataCheckpointType.STAKE_ADDRESS_TX_COUNT)
            .orElse(
                DataCheckpoint.builder()
                    .slotNo(startSlot)
                    .type(DataCheckpointType.STAKE_ADDRESS_TX_COUNT)
                    .build());

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
          selfProxyService.processAddressInSlotRange(latestProcessedSlot, endSlot);
      latestProcessedSlot = checkpoint.getSlotNo();
      endSlot = latestProcessedSlot + NUM_SLOT_INTERVAL;

      maxSafeProcessableSlot =
          Stream.of(endSlot, maxSlotFromLS, maxSlotFromLSAgg).min(Long::compareTo).get();

      if (previousValuesSafeProcessableSlot.equals(maxSafeProcessableSlot)) {
        log.error("The address tx count scheduler is stuck in a loop. --- Job:[{}]---", JOB_NAME);
        break;
      } else {
        previousValuesSafeProcessableSlot = maxSafeProcessableSlot;
      }
      maxSafeProcessableTime = cardanoConverters.slot().slotToTime(maxSafeProcessableSlot);
    }
    log.info(
        "The stake address tx count job switched to incremental mode --- Job: [{}] ---", JOB_NAME);
    selfProxyService.processAddressTxCountFromSafetySlotToTip(latestProcessedSlot, currentSlot);
    log.info(
        "Stake address tx count scheduler finished processing. The data had processed up to slot {} --- Job: [{}] ---",
        currentSlot,
        JOB_NAME);
  }

  @Transactional(value = "explorerTransactionManager", propagation = Propagation.REQUIRES_NEW)
  protected DataCheckpoint processAddressInSlotRange(Long startSlot, Long endSlot) {
    long startTime = System.currentTimeMillis();

    DataCheckpoint newCheckpoint =
        DataCheckpoint.builder()
            .slotNo(endSlot)
            .type(DataCheckpointType.STAKE_ADDRESS_TX_COUNT)
            .updateTime(Timestamp.from(Instant.now()))
            .build();

    List<StakeAddressTxCount> stakeAddressTxCounts =
        addressTxAmountRepository.getTotalTxCountByStakeAddressInSlotRange(startSlot, endSlot);

    if (CollectionUtils.isEmpty(stakeAddressTxCounts)) {
      log.info("No stake address found for slots {} to {}", startSlot, endSlot);
      jooqDataCheckpointRepository.upsertCheckpointByType(newCheckpoint);
      return newCheckpoint;
    }

    log.info(
        "Processing stake address tx count for slots {} to {}, size [{}] --- Job: [{}] ---",
        startSlot,
        endSlot,
        stakeAddressTxCounts.size(),
        JOB_NAME);

    List<StakeAddressTxCount> stakeAddressTxCountListNeedSave =
        BatchUtils.processInBatches(
            DEFAULT_BATCH_SIZE,
            stakeAddressTxCounts,
            list -> buildStakeAddressTxCountListInRollbackCaseMightNotOccur(list, endSlot),
            "AddressTxCount");

    if (!CollectionUtils.isEmpty(stakeAddressTxCountListNeedSave)) {
      stakeAddressTxCountRepository.saveAll(stakeAddressTxCountListNeedSave);
    }
    jooqDataCheckpointRepository.upsertCheckpointByType(newCheckpoint);
    log.info(
        "Processing stake address tx count for slots {} to {} took {} ms --- Job: [{}] ---",
        startSlot,
        endSlot,
        System.currentTimeMillis() - startTime,
        JOB_NAME);
    return newCheckpoint;
  }

  @Transactional(value = "explorerTransactionManager", propagation = Propagation.REQUIRES_NEW)
  protected void processAddressTxCountFromSafetySlotToTip(Long latestProcessedSlot, Long tip) {
    List<StakeAddressTxCount> stakeAddressTxCounts =
        addressTxAmountRepository.getTotalTxCountByStakeAddressInSlotRange(
            latestProcessedSlot, tip);

    if (CollectionUtils.isEmpty(stakeAddressTxCounts)) {
      log.info(
          "No stake address found for slots {} to {} --- Job: [{}] ---",
          JOB_NAME,
          latestProcessedSlot,
          tip);
      return;
    }
    log.info(
        "Processing stake address tx count for slots {} to {}, size [{}] -- Job: [{}] ---",
        JOB_NAME,
        latestProcessedSlot,
        tip,
        stakeAddressTxCounts.size());
    List<StakeAddressTxCount> stakeAddressTxCountListNeedSave =
        BatchUtils.processInBatches(
            DEFAULT_BATCH_SIZE,
            stakeAddressTxCounts,
            list -> buildStakeAddressTxCountListInRollbackCaseMightOccur(list, tip),
            JOB_NAME);
    if (!CollectionUtils.isEmpty(stakeAddressTxCountListNeedSave)) {
      stakeAddressTxCountRepository.saveAll(stakeAddressTxCountListNeedSave);
    }
  }

  private CompletableFuture<List<StakeAddressTxCount>>
      buildStakeAddressTxCountListInRollbackCaseMightNotOccur(
          List<StakeAddressTxCount> stakeAddressTxCounts, Long endSlot) {
    return CompletableFuture.supplyAsync(
        () -> {
          if (CollectionUtils.isEmpty(stakeAddressTxCounts)) {
            return null;
          }
          List<String> stakeAddresses =
              stakeAddressTxCounts.stream().map(StakeAddressTxCount::getStakeAddress).toList();
          long selectStartTime = System.currentTimeMillis();
          List<StakeAddressTxCount> existingStakeAddressTxCounts =
              stakeAddressTxCountRepository.findAllByStakeAddressIn(stakeAddresses);
          long selectEndTime = System.currentTimeMillis();
          log.info(
              "Time taken to select {} stakeAddressTxCounts: {} ms --- Job: [{}] ---",
              existingStakeAddressTxCounts.size(),
              selectEndTime - selectStartTime,
              JOB_NAME);
          Map<String, StakeAddressTxCount> existingAddressTxCountMap =
              existingStakeAddressTxCounts.stream()
                  .collect(
                      Collectors.toMap(StakeAddressTxCount::getStakeAddress, Function.identity()));
          stakeAddressTxCounts.forEach(
              stakeAddressTxCount -> {
                if (existingAddressTxCountMap.containsKey(stakeAddressTxCount.getStakeAddress())) {
                  StakeAddressTxCount existing =
                      existingAddressTxCountMap.get(stakeAddressTxCount.getStakeAddress());
                  if (existing.getIsCalculatedInIncrementalMode()
                      && Objects.isNull(existing.getPreviousSlot())) {
                    existing.setTxCount(stakeAddressTxCount.getTxCount());
                    existing.setPreviousSlot(endSlot);
                    existing.setPreviousTxCount(stakeAddressTxCount.getTxCount());
                  } else if (existing.getIsCalculatedInIncrementalMode()
                      && Objects.nonNull(existing.getPreviousSlot())) {
                    existing.setTxCount(
                        stakeAddressTxCount.getTxCount() + existing.getPreviousTxCount());
                  } else if (!existing.getIsCalculatedInIncrementalMode()
                      && Objects.nonNull(existing.getPreviousSlot())) {
                    existing.setPreviousSlot(existing.getUpdatedSlot());
                    existing.setPreviousTxCount(existing.getTxCount());
                    existing.setTxCount(stakeAddressTxCount.getTxCount() + existing.getTxCount());
                  }
                  existing.setUpdatedSlot(endSlot);
                  existing.setIsCalculatedInIncrementalMode(false);
                } else {
                  stakeAddressTxCount.setPreviousSlot(endSlot);
                  stakeAddressTxCount.setPreviousTxCount(stakeAddressTxCount.getTxCount());
                  stakeAddressTxCount.setUpdatedSlot(endSlot);
                  stakeAddressTxCount.setIsCalculatedInIncrementalMode(false);
                  existingStakeAddressTxCounts.add(stakeAddressTxCount);
                }
              });
          return existingStakeAddressTxCounts;
        });
  }

  private CompletableFuture<List<StakeAddressTxCount>>
      buildStakeAddressTxCountListInRollbackCaseMightOccur(
          List<StakeAddressTxCount> stakeAddressTxCounts, Long endSlot) {
    return CompletableFuture.supplyAsync(
        () -> {
          if (CollectionUtils.isEmpty(stakeAddressTxCounts)) {
            return null;
          }
          List<String> addresses =
              stakeAddressTxCounts.stream().map(StakeAddressTxCount::getStakeAddress).toList();
          List<StakeAddressTxCount> existingStakeAddressTxCounts =
              stakeAddressTxCountRepository.findAllByStakeAddressIn(addresses);
          Map<String, StakeAddressTxCount> existingStakeAddressTxCountMap =
              existingStakeAddressTxCounts.stream()
                  .collect(
                      Collectors.toMap(StakeAddressTxCount::getStakeAddress, Function.identity()));
          stakeAddressTxCounts.forEach(
              stakeAddressTxCount -> {
                if (existingStakeAddressTxCountMap.containsKey(
                    stakeAddressTxCount.getStakeAddress())) {
                  StakeAddressTxCount existing =
                      existingStakeAddressTxCountMap.get(stakeAddressTxCount.getStakeAddress());
                  if (existing.getIsCalculatedInIncrementalMode()
                      && Objects.nonNull(existing.getPreviousSlot())) {
                    existing.setTxCount(
                        stakeAddressTxCount.getTxCount() + existing.getPreviousTxCount());
                  } else if (existing.getIsCalculatedInIncrementalMode()
                      && Objects.isNull(existing.getPreviousSlot())) {
                    existing.setTxCount(stakeAddressTxCount.getTxCount());
                  } else if (!existing.getIsCalculatedInIncrementalMode()) {
                    existing.setPreviousSlot(existing.getUpdatedSlot());
                    existing.setPreviousTxCount(existing.getTxCount());
                    existing.setTxCount(stakeAddressTxCount.getTxCount() + existing.getTxCount());
                  }
                  existing.setUpdatedSlot(endSlot);
                  existing.setIsCalculatedInIncrementalMode(true);
                } else {
                  stakeAddressTxCount.setUpdatedSlot(endSlot);
                  stakeAddressTxCount.setIsCalculatedInIncrementalMode(true);
                  existingStakeAddressTxCounts.add(stakeAddressTxCount);
                }
              });
          return existingStakeAddressTxCounts;
        });
  }
}
