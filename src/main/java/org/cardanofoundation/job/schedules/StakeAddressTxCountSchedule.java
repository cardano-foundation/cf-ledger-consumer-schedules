package org.cardanofoundation.job.schedules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.common.entity.ledgersync.BaseEntity_;
import org.cardanofoundation.explorer.common.entity.ledgersync.Block;
import org.cardanofoundation.job.common.constant.Constant;
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.StakeAddressRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.jooq.JOOQStakeAddressTxCountRepository;
import org.cardanofoundation.job.util.BatchUtils;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@ConditionalOnProperty(
        value = "jobs.address-tx-count.enabled",
        matchIfMissing = true,
        havingValue = "true")
public class StakeAddressTxCountSchedule {

  @Value("${application.network}")
  private String network;

  private final AddressTxAmountRepository addressTxAmountRepository;
  private final StakeAddressRepository stakeAddressRepository;
  private final JOOQStakeAddressTxCountRepository jooqStakeAddressTxCountRepository;
  private final BlockRepository blockRepository;
  private final RedisTemplate<String, Integer> redisTemplate;

  private static final int DEFAULT_PAGE_SIZE = 1000;

  private String getRedisKey(String prefix) {
    return prefix + "_" + network;
  }

  @Scheduled(initialDelay = 10000, fixedDelayString = "${jobs.address-tx-count.fixed-delay}")
  @Transactional
  public void syncStakeAddressTxCount() {
    Optional<Block> latestBlock = blockRepository.findLatestBlock();
    if (latestBlock.isEmpty()) {
      return;
    }

    final String stakeAddressTxCountCheckPoint =
        getRedisKey(RedisKey.STAKE_ADDRESS_TX_COUNT_CHECKPOINT.name());
    final long currentMaxSlotNo =
        Math.min(latestBlock.get().getSlotNo(), addressTxAmountRepository.getMaxSlotNoFromCursor());
    final Integer checkpoint = redisTemplate.opsForValue().get(stakeAddressTxCountCheckPoint);
    if (Objects.isNull(checkpoint)) {
      init();
    } else if (currentMaxSlotNo > checkpoint.longValue()) {
      update(checkpoint.longValue(), currentMaxSlotNo);
    }

    // Update the checkpoint to the currentMaxSlotNo - 43200 to avoid missing any data when node
    // rollback
    redisTemplate
        .opsForValue()
        .set(
            stakeAddressTxCountCheckPoint,
            Math.max((int) currentMaxSlotNo - Constant.ROLLBACKSLOT, 0));
  }

  public void init() {
    log.info("Start init StakeAddressTxCount");
    long startTime = System.currentTimeMillis();
    long index = 1;
    List<CompletableFuture<List<Void>>> savingStakeAddressTxCountFutures = new ArrayList<>();

    Pageable pageable =
        PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, BaseEntity_.ID));
    Slice<String> stakeAddressSlice = stakeAddressRepository.getStakeAddressViews(pageable);
    List<String> firstStakeAddresses = stakeAddressSlice.getContent();
    savingStakeAddressTxCountFutures.add(
        CompletableFuture.supplyAsync(
            () -> {
              jooqStakeAddressTxCountRepository.insertStakeAddressTxCount(firstStakeAddresses);
              return null;
            }));

    while (stakeAddressSlice.hasNext()) {
      stakeAddressSlice =
          stakeAddressRepository.getStakeAddressViews(stakeAddressSlice.nextPageable());
      List<String> stakeAddresses = stakeAddressSlice.getContent();

      CompletableFuture<List<Void>> savingStakeAddressTxCountFuture =
          CompletableFuture.supplyAsync(
              () -> {
                jooqStakeAddressTxCountRepository.insertStakeAddressTxCount(stakeAddresses);
                return null;
              });

      savingStakeAddressTxCountFutures.add(savingStakeAddressTxCountFuture);

      index++;
      // After every 5 batches, insert the fetched token tx count data into the database in batches.
      if (savingStakeAddressTxCountFutures.size() % 5 == 0) {
        CompletableFuture.allOf(savingStakeAddressTxCountFutures.toArray(new CompletableFuture[0]))
            .join();
        savingStakeAddressTxCountFutures.clear();
        log.info("Total saved stake address tx count: {}", index * DEFAULT_PAGE_SIZE);
      }
    }

    // Insert the remaining token tx count data into the database.
    CompletableFuture.allOf(savingStakeAddressTxCountFutures.toArray(new CompletableFuture[0]))
        .join();
    log.info("End init StakeAddressTxCount in {} ms", System.currentTimeMillis() - startTime);
  }

  private void update(Long slotNoCheckpoint, Long currentMaxSlotNo) {
    log.info("Start update StakeAddressTxCount");
    long startTime = System.currentTimeMillis();
    List<String> stakeAddressInvolvedInTx =
        addressTxAmountRepository.findStakeAddressBySlotNoBetween(
            slotNoCheckpoint, currentMaxSlotNo);

    List<CompletableFuture<List<Void>>> savingStakeAddressTxCountFutures = new ArrayList<>();

    BatchUtils.doBatching(
        100,
        stakeAddressInvolvedInTx,
        stakeAddresses -> {
          CompletableFuture<List<Void>> savingStakeAddressTxCountFuture =
              CompletableFuture.supplyAsync(
                  () -> {
                    jooqStakeAddressTxCountRepository.insertStakeAddressTxCount(stakeAddresses);
                    return null;
                  });

          savingStakeAddressTxCountFutures.add(savingStakeAddressTxCountFuture);

          // After every 5 batches, insert the fetched stake address tx count data into the database
          // in batches.
          if (savingStakeAddressTxCountFutures.size() % 5 == 0) {
            CompletableFuture.allOf(
                    savingStakeAddressTxCountFutures.toArray(new CompletableFuture[0]))
                .join();
            savingStakeAddressTxCountFutures.clear();
          }
        });

    // Insert the remaining stake address tx count data into the database.
    CompletableFuture.allOf(savingStakeAddressTxCountFutures.toArray(new CompletableFuture[0]))
        .join();

    log.info(
        "End update StakeAddressTxCount with size = {} in {} ms",
        stakeAddressInvolvedInTx.size(),
        System.currentTimeMillis() - startTime);
  }
}
