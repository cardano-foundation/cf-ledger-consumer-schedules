package org.cardanofoundation.job.schedules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.common.entity.ledgersync.Block;
import org.cardanofoundation.job.common.constant.Constant;
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxCountRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.jooq.JOOQAddressTxCountRepository;
import org.cardanofoundation.job.util.BatchUtils;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class AddressTxCountSchedule {

  @Value("${application.network}")
  private String network;

  private final JOOQAddressTxCountRepository jooqAddressTxCountRepository;
  private final AddressTxAmountRepository addressTxAmountRepository;
  private final BlockRepository blockRepository;
  private final AddressTxCountRepository addressTxCountRepository;
  private final RedisTemplate<String, Integer> redisTemplate;

  private String getRedisKey(String prefix) {
    return prefix + "_" + network;
  }

  @Scheduled(initialDelay = 10000, fixedDelayString = "${jobs.address-tx-count.fixed-delay}")
  @Transactional
  public void syncAddressTxCount() {
    final String addressTxCountCheckPoint =
        getRedisKey(RedisKey.ADDRESS_TX_COUNT_CHECKPOINT.name());

    Optional<Block> latestBlock = blockRepository.findLatestBlock();
    if (latestBlock.isEmpty()) {
      return;
    }

    final long currentMaxSlotNo =
        Math.min(latestBlock.get().getSlotNo(), addressTxAmountRepository.getMaxSlotNoFromCursor());

    final Integer checkpoint = redisTemplate.opsForValue().get(addressTxCountCheckPoint);
    if (Objects.isNull(checkpoint) || addressTxCountRepository.count() == 0) {
      init();
    } else if (currentMaxSlotNo > checkpoint.longValue()) {
      update(checkpoint.longValue(), currentMaxSlotNo);
    }

    // Update the checkpoint to the currentMaxSlotNo - 43200 to avoid missing any data when node
    // rollback
    redisTemplate
        .opsForValue()
        .set(addressTxCountCheckPoint, Math.max((int) currentMaxSlotNo - Constant.ROLLBACKSLOT, 0));
  }

  public void init() {
    log.info("Start init AddressTxCount");
    long startTime = System.currentTimeMillis();

    final int numberOfThreads = 10;
    Long totalAddresses = addressTxAmountRepository.getMaxAddressId();
    if (totalAddresses == null) {
      return;
    }

    long partitionSize = totalAddresses / numberOfThreads;
    int insertBatchSize = 1000;
    insertAddressTxCountParallel(partitionSize, insertBatchSize, numberOfThreads, totalAddresses);

    log.info("End init AddressTxCount in {} ms", System.currentTimeMillis() - startTime);
  }

  private void update(Long slotNoCheckpoint, Long currentMaxSlotNo) {
    log.info("Start update AddressTxCount");
    long startTime = System.currentTimeMillis();
    List<CompletableFuture<List<Void>>> savingAddressTxCountFutures = new ArrayList<>();

    List<String> addressInvolvedInTx =
        addressTxAmountRepository.findAddressBySlotNoBetween(slotNoCheckpoint, currentMaxSlotNo);

    log.info(
        "addressInBlockRange from blockCheckpoint {} to {}, size: {}",
        slotNoCheckpoint,
        currentMaxSlotNo,
        addressInvolvedInTx.size());

    BatchUtils.doBatching(
        100,
        addressInvolvedInTx,
        stakeAddresses -> {
          CompletableFuture<List<Void>> savingStakeAddressTxCountFuture =
              CompletableFuture.supplyAsync(
                  () -> {
                    jooqAddressTxCountRepository.updateAddressTxCount(stakeAddresses);
                    return null;
                  });

          savingAddressTxCountFutures.add(savingStakeAddressTxCountFuture);

          // After every 5 batches, insert the fetched stake address tx count data into the database
          // in batches.
          if (savingAddressTxCountFutures.size() % 5 == 0) {
            CompletableFuture.allOf(savingAddressTxCountFutures.toArray(new CompletableFuture[0]))
                .join();
            savingAddressTxCountFutures.clear();
          }
        });

    // Insert the remaining stake address tx count data into the database.
    CompletableFuture.allOf(savingAddressTxCountFutures.toArray(new CompletableFuture[0])).join();

    log.info(
        "End update AddressTxCount with size = {} in {} ms",
        addressInvolvedInTx.size(),
        System.currentTimeMillis() - startTime);
  }

  @SneakyThrows
  private void insertAddressTxCountParallel(
      long partitionSize, int batchSize, int numberOfThreads, long totalAddresses) {

    List<CompletableFuture> futures = new ArrayList<>();
    for (int i = 0; i < numberOfThreads; i++) {
      long startId = (i * partitionSize) + 1; // ID starts from 1
      long endId =
          (i == numberOfThreads - 1)
              ? totalAddresses
              : startId + partitionSize - 1; // ensure endId does not exceed totalAddresses

      CompletableFuture<Boolean> completableFuture =
          CompletableFuture.supplyAsync(
              () -> {
                jooqAddressTxCountRepository.insertAddressTxCount(startId, endId, batchSize);
                return true;
              });

      futures.add(completableFuture);
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    for (var future : futures) {
      future.get();
    }
  }
}
