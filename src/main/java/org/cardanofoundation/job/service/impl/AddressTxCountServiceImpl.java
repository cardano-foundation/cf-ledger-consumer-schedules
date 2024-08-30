package org.cardanofoundation.job.service.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.AddressTxCount;
import org.cardanofoundation.job.repository.explorer.DataCheckpointRepository;
import org.cardanofoundation.job.repository.explorer.jooq.JOOQDataCheckpointRepository;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxCountRepository;
import org.cardanofoundation.job.service.AddressTxCountService;
import org.cardanofoundation.job.util.BatchUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressTxCountServiceImpl implements AddressTxCountService {

  @Autowired @Lazy AddressTxCountServiceImpl selfProxyService;
  private final JOOQDataCheckpointRepository jooqDataCheckpointRepository;
  private final CardanoConverters cardanoConverters;
  private final BlockRepository blockRepository;
  private final AddressTxAmountRepository addressTxAmountRepository;
  private final DataCheckpointRepository dataCheckpointRepository;
  private final AddressTxCountRepository addressTxCountRepository;
  private static final Integer DEFAULT_BATCH_SIZE = 500;
  private final String JOB_NAME = "AddressTxCount";

  @Value("${jobs.address-tx-count.num-slot-interval}")
  private Integer NUM_SLOT_INTERVAL;

  @Override
  public void updateAddressTxCount() {
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
            .findFirstByType(DataCheckpointType.ADDRESS_TX_COUNT)
            .orElse(
                DataCheckpoint.builder()
                    .slotNo(startSlot)
                    .type(DataCheckpointType.ADDRESS_TX_COUNT)
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
        log.error(
            "No progress in processing token tx count. Breaking the loop --- Job: [{}] ---",
            JOB_NAME);
      } else {
        previousValuesSafeProcessableSlot = maxSafeProcessableSlot;
      }
      maxSafeProcessableTime = cardanoConverters.slot().slotToTime(maxSafeProcessableSlot);
    }
    log.info("The address tx count job switched to incremental mode --- Job: [{}] ---", JOB_NAME);
    selfProxyService.processAddressTxCountFromSafetySlotToTip(latestProcessedSlot, currentSlot);
    log.info(
        "Address tx count scheduler finished processing. The data had processed up to slot {} --- Job: [{}] ---",
        currentSlot,
        JOB_NAME);
  }

  @Transactional(value = "explorerTransactionManager", propagation = Propagation.REQUIRES_NEW)
  protected DataCheckpoint processAddressInSlotRange(Long startSlot, Long endSlot) {
    long startTime = System.currentTimeMillis();

    DataCheckpoint newCheckpoint =
        DataCheckpoint.builder()
            .slotNo(endSlot)
            .type(DataCheckpointType.ADDRESS_TX_COUNT)
            .updateTime(Timestamp.from(Instant.now()))
            .build();

    List<AddressTxCount> addressTxCounts =
        addressTxAmountRepository.getTotalTxCountByAddressInSlotRange(startSlot, endSlot);

    if (CollectionUtils.isEmpty(addressTxCounts)) {
      log.info("No address found for slots {} to {}", startSlot, endSlot);
      jooqDataCheckpointRepository.upsertCheckpointByType(newCheckpoint);
      return newCheckpoint;
    }

    log.info(
        "Processing address tx count for slots {} to {}, size [{}] --- Job: [{}] ---",
        startSlot,
        endSlot,
        addressTxCounts.size(),
        JOB_NAME);

    List<AddressTxCount> addressTxCountListNeedSave =
        BatchUtils.processInBatches(
            DEFAULT_BATCH_SIZE,
            addressTxCounts,
            list -> buildAddressTxCountListInRollbackCaseMightNotOccur(list, endSlot),
            "AddressTxCount");

    if (!CollectionUtils.isEmpty(addressTxCountListNeedSave)) {
      addressTxCountRepository.saveAll(addressTxCountListNeedSave);
    }
    jooqDataCheckpointRepository.upsertCheckpointByType(newCheckpoint);
    log.info(
        "Processing address tx count for slots {} to {} took {} ms --- Job: [{}] ---",
        startSlot,
        endSlot,
        System.currentTimeMillis() - startTime,
        JOB_NAME);
    return newCheckpoint;
  }

  @Transactional(value = "explorerTransactionManager", propagation = Propagation.REQUIRES_NEW)
  protected void processAddressTxCountFromSafetySlotToTip(Long latestProcessedSlot, Long tip) {
    List<AddressTxCount> addressTxCounts =
        addressTxAmountRepository.getTotalTxCountByAddressInSlotRange(latestProcessedSlot, tip);

    if (CollectionUtils.isEmpty(addressTxCounts)) {
      log.info(
          "No address found for slots {} to {} --- Job: [{}] ---",
          JOB_NAME,
          latestProcessedSlot,
          tip);
      return;
    }
    log.info(
        "Processing address tx count for slots {} to {}, size [{}] -- Job: [{}] ---",
        JOB_NAME,
        latestProcessedSlot,
        tip,
        addressTxCounts.size());
    List<AddressTxCount> addressTxCountListNeedSave =
        BatchUtils.processInBatches(
            DEFAULT_BATCH_SIZE,
            addressTxCounts,
            list -> buildAddressTxCountListInRollbackCaseMightOccur(list, tip),
            "AddressTxCount");
    if (!CollectionUtils.isEmpty(addressTxCountListNeedSave)) {
      addressTxCountRepository.saveAll(addressTxCountListNeedSave);
    }
  }

  private CompletableFuture<List<AddressTxCount>>
      buildAddressTxCountListInRollbackCaseMightNotOccur(
          List<AddressTxCount> addressTxCounts, Long endSlot) {
    return CompletableFuture.supplyAsync(
        () -> {
          if (CollectionUtils.isEmpty(addressTxCounts)) {
            return null;
          }
          List<String> addresses =
              addressTxCounts.stream().map(AddressTxCount::getAddress).toList();
          long selectStartTime = System.currentTimeMillis();
          List<AddressTxCount> existingTokenTxCounts =
              addressTxCountRepository.findAllByAddressIn(addresses);
          long selectEndTime = System.currentTimeMillis();
          log.info(
              "Time taken to select {} addressTxCounts: {} ms --- Job: [{}] ---",
              existingTokenTxCounts.size(),
              selectEndTime - selectStartTime,
              JOB_NAME);
          Map<String, AddressTxCount> existingAddressTxCountMap =
              existingTokenTxCounts.stream()
                  .collect(Collectors.toMap(AddressTxCount::getAddress, Function.identity()));
          addressTxCounts.forEach(
              addressTxCount -> {
                if (existingAddressTxCountMap.containsKey(addressTxCount.getAddress())) {
                  AddressTxCount existing =
                      existingAddressTxCountMap.get(addressTxCount.getAddress());
                  if (existing.getIsCalculatedInIncrementalMode()
                      && Objects.isNull(existing.getPreviousSlot())) {
                    existing.setTxCount(addressTxCount.getTxCount());
                    existing.setPreviousSlot(endSlot);
                    existing.setPreviousTxCount(addressTxCount.getTxCount());
                  } else if (existing.getIsCalculatedInIncrementalMode()
                      && Objects.nonNull(existing.getPreviousSlot())) {
                    existing.setTxCount(
                        addressTxCount.getTxCount() + existing.getPreviousTxCount());
                  } else if (!existing.getIsCalculatedInIncrementalMode()
                      && Objects.nonNull(existing.getPreviousSlot())) {
                    existing.setPreviousSlot(existing.getUpdatedSlot());
                    existing.setPreviousTxCount(existing.getTxCount());
                    existing.setTxCount(addressTxCount.getTxCount() + existing.getTxCount());
                  }
                  existing.setUpdatedSlot(endSlot);
                  existing.setIsCalculatedInIncrementalMode(false);
                } else {
                  addressTxCount.setPreviousSlot(endSlot);
                  addressTxCount.setPreviousTxCount(addressTxCount.getTxCount());
                  addressTxCount.setUpdatedSlot(endSlot);
                  addressTxCount.setIsCalculatedInIncrementalMode(false);
                  existingTokenTxCounts.add(addressTxCount);
                }
              });
          return existingTokenTxCounts;
        });
  }

  private CompletableFuture<List<AddressTxCount>> buildAddressTxCountListInRollbackCaseMightOccur(
      List<AddressTxCount> addressTxCounts, Long endSlot) {
    return CompletableFuture.supplyAsync(
        () -> {
          if (CollectionUtils.isEmpty(addressTxCounts)) {
            return null;
          }
          List<String> addresses =
              addressTxCounts.stream().map(AddressTxCount::getAddress).toList();
          List<AddressTxCount> existingAddressTxCounts =
              addressTxCountRepository.findAllByAddressIn(addresses);
          Map<String, AddressTxCount> existingAddressTxCountMap =
              existingAddressTxCounts.stream()
                  .collect(Collectors.toMap(AddressTxCount::getAddress, Function.identity()));
          addressTxCounts.forEach(
              addressTxCount -> {
                if (existingAddressTxCountMap.containsKey(addressTxCount.getAddress())) {
                  AddressTxCount existing =
                      existingAddressTxCountMap.get(addressTxCount.getAddress());
                  if (existing.getIsCalculatedInIncrementalMode()
                      && Objects.nonNull(existing.getPreviousSlot())) {
                    existing.setTxCount(
                        addressTxCount.getTxCount() + existing.getPreviousTxCount());
                  } else if (existing.getIsCalculatedInIncrementalMode()
                      && Objects.isNull(existing.getPreviousSlot())) {
                    existing.setTxCount(addressTxCount.getTxCount());
                  } else if (!existing.getIsCalculatedInIncrementalMode()) {
                    existing.setPreviousSlot(existing.getUpdatedSlot());
                    existing.setPreviousTxCount(existing.getTxCount());
                    existing.setTxCount(addressTxCount.getTxCount() + existing.getTxCount());
                  }
                  existing.setUpdatedSlot(endSlot);
                  existing.setIsCalculatedInIncrementalMode(true);
                } else {
                  addressTxCount.setUpdatedSlot(endSlot);
                  addressTxCount.setIsCalculatedInIncrementalMode(true);
                  existingAddressTxCounts.add(addressTxCount);
                }
              });
          return existingAddressTxCounts;
        });
  }
}
