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
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.jooq.JOOQLatestTokenBalanceRepository;
import org.cardanofoundation.job.util.BatchUtils;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class LatestTokenBalanceSchedule {

  private final AddressTxAmountRepository addressTxAmountRepository;

  @Value("${application.network}")
  private String network;

  private final AddressRepository addressRepository;
  private final JOOQLatestTokenBalanceRepository jooqLatestTokenBalanceRepository;
  private final BlockRepository blockRepository;
  private final RedisTemplate<String, Integer> redisTemplate;

  private static final int DEFAULT_PAGE_SIZE = 10000;

  private String getRedisKey(String prefix) {
    return prefix + "_" + network;
  }

  @Scheduled(fixedDelayString = "${jobs.latest-token-balance.fixed-delay}")
  @Transactional
  public void syncLatestTokenBalance() {
    final String latestTokenBalanceCheckpoint =
        getRedisKey(RedisKey.LATEST_TOKEN_BALANCE_CHECKPOINT.name());

    Optional<Block> latestBlock = blockRepository.findLatestBlock();
    if (latestBlock.isEmpty()) {
      return;
    }
    final long currentMaxBlockNo =
        Math.min(
            addressTxAmountRepository.getMaxBlockNoFromCursor(), latestBlock.get().getBlockNo());
    final Integer checkpoint = redisTemplate.opsForValue().get(latestTokenBalanceCheckpoint);
    if (Objects.isNull(checkpoint)) {
      init();
    } else if (currentMaxBlockNo > checkpoint.longValue()) {
      update(checkpoint.longValue(), currentMaxBlockNo);
    }

    // Update the checkpoint to the currentMaxBlockNo - 50 to avoid missing any data when node
    // rollback
    redisTemplate
        .opsForValue()
        .set(latestTokenBalanceCheckpoint, Math.max((int) currentMaxBlockNo - 2160, 0));
  }

  private void init() {
    log.info("Start init LatestTokenBalance");
    long startTime = System.currentTimeMillis();

    // Drop all indexes before inserting the latest token balance data into the database.
    jooqLatestTokenBalanceRepository.dropAllIndexes();
    List<CompletableFuture<List<Void>>> savingLatestTokenBalanceFutures = new ArrayList<>();
    long index = 1;
    Pageable pageable =
        PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, BaseEntity_.ID));
    Slice<String> addressSlice = addressRepository.getAddressBySlice(pageable);
    addLatestTokenBalanceFutures(savingLatestTokenBalanceFutures, addressSlice.getContent());

    while (addressSlice.hasNext()) {
      addressSlice = addressRepository.getAddressBySlice(addressSlice.nextPageable());
      addLatestTokenBalanceFutures(savingLatestTokenBalanceFutures, addressSlice.getContent());
      index++;
      // After every 5 batches, insert the fetched latest token balance data into the database
      // in batches.
      if (savingLatestTokenBalanceFutures.size() % 5 == 0) {
        log.info("Inserting latest token balance data into the database");
        CompletableFuture.allOf(savingLatestTokenBalanceFutures.toArray(new CompletableFuture[0]))
            .join();
        savingLatestTokenBalanceFutures.clear();
        log.info("Total latest token balance: {}", index * DEFAULT_PAGE_SIZE);
      }
    }

    // Insert the remaining latest token balance data into the database in batches.
    CompletableFuture.allOf(savingLatestTokenBalanceFutures.toArray(new CompletableFuture[0]))
        .join();

    // Create all indexes after inserting the latest token balance data into the database.
    jooqLatestTokenBalanceRepository.createAllIndexes();
    log.info("End update LatestTokenBalance in {} ms", System.currentTimeMillis() - startTime);
  }

  private void addLatestTokenBalanceFutures(
      List<CompletableFuture<List<Void>>> savingLatestTokenBalanceFutures, List<String> addresses) {
    savingLatestTokenBalanceFutures.add(
        CompletableFuture.supplyAsync(
            () -> {
              jooqLatestTokenBalanceRepository.insertLatestTokenBalanceByAddressIn(addresses);
              return null;
            }));
  }

  private void update(Long blockNoCheckpoint, Long currentMaxBlockNo) {
    log.info("Start update LatestTokenBalance");
    long startTime = System.currentTimeMillis();
    List<CompletableFuture<List<Void>>> savingLatestTokenBalanceFutures = new ArrayList<>();
    Long epochBlockTimeCheckpoint =
        blockRepository.getBlockTimeByBlockNo(blockNoCheckpoint).toInstant().getEpochSecond();
    Long epochBlockTimeCurrent =
        blockRepository.getBlockTimeByBlockNo(currentMaxBlockNo).toInstant().getEpochSecond();

    List<String> addressInvolvedInTx =
        addressTxAmountRepository.findAddressBySlotNoBetween(
            epochBlockTimeCheckpoint, epochBlockTimeCurrent);

    log.info(
        "addressInBlockRange from blockCheckpoint {} to {}, size: {}",
        epochBlockTimeCheckpoint,
        epochBlockTimeCurrent,
        addressInvolvedInTx.size());

    BatchUtils.doBatching(
        100,
        addressInvolvedInTx,
        address -> {
          addLatestTokenBalanceFutures(savingLatestTokenBalanceFutures, address);

          // After every 5 batches, insert the fetched stake address tx count data into the database
          // in batches.
          if (savingLatestTokenBalanceFutures.size() % 5 == 0) {
            CompletableFuture.allOf(
                    savingLatestTokenBalanceFutures.toArray(new CompletableFuture[0]))
                .join();
            savingLatestTokenBalanceFutures.clear();
          }
        });

    // Insert the remaining stake address tx count data into the database.
    CompletableFuture.allOf(savingLatestTokenBalanceFutures.toArray(new CompletableFuture[0]))
        .join();

    log.info(
        "End update LatestTokenBalance with address size = {} in {} ms",
        addressInvolvedInTx.size(),
        System.currentTimeMillis() - startTime);
  }
}
