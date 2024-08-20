package org.cardanofoundation.job.schedules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
import org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount;
import org.cardanofoundation.job.common.constant.Constant;
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.MultiAssetRepository;
import org.cardanofoundation.job.repository.ledgersync.TokenTxCountRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.LatestTokenBalanceRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.jooq.JOOQAddressBalanceRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.jooq.JOOQLatestTokenBalanceRepository;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@ConditionalOnProperty(
        value = "jobs.latest-token-balance.enabled",
        matchIfMissing = true,
        havingValue = "true")
public class LatestTokenBalanceSchedule {

  private static final int DEFAULT_PAGE_SIZE = 1000;
  private final AddressTxAmountRepository addressTxAmountRepository;
  private final TokenTxCountRepository tokenTxCountRepository;
  private final MultiAssetRepository multiAssetRepository;
  private final JOOQLatestTokenBalanceRepository jooqLatestTokenBalanceRepository;
  private final JOOQAddressBalanceRepository jooqAddressBalanceRepository;
  private final LatestTokenBalanceRepository latestTokenBalanceRepository;
  private final BlockRepository blockRepository;
  private final RedisTemplate<String, Integer> redisTemplate;

  @Value("${application.network}")
  private String network;

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

    final long currentMaxSlotNo =
        Math.min(addressTxAmountRepository.getMaxSlotNoFromCursor(), latestBlock.get().getSlotNo());
    final Integer checkpoint = redisTemplate.opsForValue().get(latestTokenBalanceCheckpoint);

    if (Objects.isNull(checkpoint) || latestTokenBalanceRepository.count() == 0) {
      init(currentMaxSlotNo);
    } else if (currentMaxSlotNo > checkpoint.longValue()) {
      update(checkpoint.longValue(), currentMaxSlotNo);
    }

    // Update the checkpoint to the currentMaxSlotNo - 43200 (slot) to avoid missing any data when
    // node
    // rollback
    redisTemplate
        .opsForValue()
        .set(
            latestTokenBalanceCheckpoint,
            Math.max((int) currentMaxSlotNo - Constant.ROLLBACKSLOT, 0));
  }

  private void init(long currentMaxSlotNo) {
    log.info("Start init LatestTokenBalance");
    long startTime = System.currentTimeMillis();

    // Drop all indexes before inserting the latest token balance data into the database.
    jooqLatestTokenBalanceRepository.dropAllIndexes();
    long index = 1;
    Pageable pageable =
        PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, BaseEntity_.ID));
    Slice<String> multiAssetSlice = multiAssetRepository.getTokenUnitSlice(pageable);
    processingLatestTokenBalance(multiAssetSlice.getContent(), 0L, false);

    while (multiAssetSlice.hasNext()) {
      multiAssetSlice = multiAssetRepository.getTokenUnitSlice(multiAssetSlice.nextPageable());
      processingLatestTokenBalance(multiAssetSlice.getContent(), 0L, false);
      index++;
      log.info("Total processed units: {}", index * DEFAULT_PAGE_SIZE);
    }

    // Create all indexes after inserting the latest token balance data into the database.
    jooqLatestTokenBalanceRepository.createAllIndexes();
    jooqLatestTokenBalanceRepository.deleteAllZeroHolders();
    jooqAddressBalanceRepository.deleteAllZeroHolders(currentMaxSlotNo);
    log.info("End init LatestTokenBalance in {} ms", System.currentTimeMillis() - startTime);
  }

  private void addLatestTokenBalanceFutures(
      List<CompletableFuture<List<Void>>> savingLatestTokenBalanceFutures,
      List<String> units,
      Long slotCheckpoint,
      boolean includeZeroHolders) {
    savingLatestTokenBalanceFutures.add(
        CompletableFuture.supplyAsync(
            () -> {
              jooqLatestTokenBalanceRepository.insertLatestTokenBalanceByUnitIn(
                  units, slotCheckpoint, includeZeroHolders);
              return null;
            }));
  }

  private void update(Long slotNoCheckpoint, Long currentMaxSlotNo) {
    log.info("Start update LatestTokenBalance");
    long startTime = System.currentTimeMillis();
    List<String> unitsInBlockRange =
        addressTxAmountRepository.findUnitBySlotInRange(slotNoCheckpoint, currentMaxSlotNo);

    log.info(
        "unitsInBlockRange from slotNoCheckpoint {} to {}, size: {}",
        slotNoCheckpoint,
        currentMaxSlotNo,
        unitsInBlockRange.size());

    processingLatestTokenBalance(unitsInBlockRange, slotNoCheckpoint, true);

    jooqLatestTokenBalanceRepository.deleteAllZeroHolders();
    jooqAddressBalanceRepository.deleteAllZeroHolders(currentMaxSlotNo);

    log.info(
        "End update LatestTokenBalance with address size = {} in {} ms",
        unitsInBlockRange.size(),
        System.currentTimeMillis() - startTime);
  }

  /**
   * Processes and saves the latest token balance information for a specified range of processing
   * units. This method aggregates transaction counts for tokens across specified processing units
   * and initiates asynchronous operations to save these aggregated balances. <br>
   * It ensures that the operation is only initiated when the cumulative transaction count reaches a
   * predefined threshold or after every 5 batches to avoid overwhelming the system with too many
   * concurrent database writes. <br>
   * Additionally, it handles the final batch of transactions that may not reach the threshold but
   * still needs to be processed. <br>
   * The method supports an optional parameter to include zero holders in the processing, which can
   * be useful for accounting purposes even if no transactions occurred for those units.
   *
   * @param units A list of strings representing the identifiers of the processing units whose
   *     transaction counts are being aggregated and saved.
   * @param slotCheckpoint A timestamp indicating the point in time at which the latest token
   *     balances were captured. This is used as part of the data being saved.
   * @param includeZeroHolders A boolean flag indicating whether the processing should include units
   *     with zero transaction counts. This can be useful for maintaining accurate records even for
   *     units with no activity.
   */
  private void processingLatestTokenBalance(
      List<String> units, Long slotCheckpoint, boolean includeZeroHolders) {
    List<TokenTxCount> tokenTxCounts = getTokenTxCountOrderedByTxCount(units);
    List<CompletableFuture<List<Void>>> savingLatestTokenBalanceFutures = new ArrayList<>();

    // This variable holds the threshold value for the total transaction count before batching
    // starts.
    int sumTxCountThreshold = 5000;

    // Keeps track of the cumulative transaction count for the current batch being processed.
    int currentSumTxCount = 0;

    // A list to hold the identifiers of the processing units currently being processed in the
    // batch.
    List<String> currentProcessingUnits = new ArrayList<>();

    for (TokenTxCount tokenTxCount : tokenTxCounts) {
      currentProcessingUnits.add(tokenTxCount.getUnit());

      // If the cumulative transaction count exceeds the threshold, insert the fetched latest token
      if (currentSumTxCount + tokenTxCount.getTxCount() > sumTxCountThreshold) {
        addLatestTokenBalanceFutures(
            savingLatestTokenBalanceFutures,
            new ArrayList<>(currentProcessingUnits),
            slotCheckpoint,
            includeZeroHolders);
        currentProcessingUnits.clear();
        currentSumTxCount = 0;
      } else {
        currentSumTxCount += tokenTxCount.getTxCount();
      }

      // After every 10 batches, insert the fetched latest token balance data into the database
      // in batches.
      if (!savingLatestTokenBalanceFutures.isEmpty()
          && savingLatestTokenBalanceFutures.size() % 10 == 0) {
        CompletableFuture.allOf(savingLatestTokenBalanceFutures.toArray(new CompletableFuture[0]))
            .join();
        savingLatestTokenBalanceFutures.clear();
      }
    }

    // Check if there are any remaining units to process.
    if (!currentProcessingUnits.isEmpty()) {
      addLatestTokenBalanceFutures(
          savingLatestTokenBalanceFutures,
          new ArrayList<>(currentProcessingUnits),
          slotCheckpoint,
          includeZeroHolders);
    }
    // Insert the remaining stake address tx count data into the database.
    CompletableFuture.allOf(savingLatestTokenBalanceFutures.toArray(new CompletableFuture[0]))
        .join();
  }

  private List<TokenTxCount> getTokenTxCountOrderedByTxCount(List<String> units) {
    List<TokenTxCount> tokenTxCounts = tokenTxCountRepository.findAllByUnitIn(units);

    Map<String, TokenTxCount> tokenTxCountMap =
        tokenTxCounts.stream()
            .collect(Collectors.toMap(TokenTxCount::getUnit, tokenTxCount -> tokenTxCount));

    // put all units with not exist tx count to ZERO
    units.forEach(unit -> tokenTxCountMap.putIfAbsent(unit, new TokenTxCount(unit, 0L)));

    // return the list of TokenTxCount with tx_count asc order
    return tokenTxCountMap.values().stream()
        .sorted((t1, t2) -> (int) (t1.getTxCount() - t2.getTxCount()))
        .collect(Collectors.toList());
  }
}
