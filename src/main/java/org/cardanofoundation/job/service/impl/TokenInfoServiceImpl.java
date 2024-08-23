package org.cardanofoundation.job.service.impl;

import static org.cardanofoundation.job.common.enumeration.RedisKey.AGGREGATED_CACHE;
import static org.cardanofoundation.job.common.enumeration.RedisKey.TOTAL_TOKEN_COUNT;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.explorer.common.entity.explorer.TokenInfo;
import org.cardanofoundation.explorer.common.entity.explorer.TokenInfoCheckpoint;
import org.cardanofoundation.explorer.common.entity.ledgersync.Block;
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.repository.explorer.TokenInfoCheckpointRepository;
import org.cardanofoundation.job.repository.explorer.TokenInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.MultiAssetRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.LatestTokenBalanceRepository;
import org.cardanofoundation.job.service.TokenInfoService;
import org.cardanofoundation.job.service.TokenInfoServiceAsync;
import org.cardanofoundation.job.util.BatchUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenInfoServiceImpl implements TokenInfoService {

  @Autowired @Lazy TokenInfoServiceImpl selfProxyService;

  private final TokenInfoCheckpointRepository tokenInfoCheckpointRepository;
  private final BlockRepository blockRepository;
  private final MultiAssetRepository multiAssetRepository;
  private final TokenInfoServiceAsync tokenInfoServiceAsync;
  private final LatestTokenBalanceRepository latestTokenBalanceRepository;
  private final CardanoConverters cardanoConverters;
  private final TokenInfoRepository tokenInfoRepository;
  private final AddressTxAmountRepository addressTxAmountRepository;

  @Value("${jobs.token-info.num-slot-interval}")
  private Integer NUM_SLOT_INTERVAL;

  private final RedisTemplate<String, Integer> redisTemplate;

  private static final int DEFAULT_BATCH_SIZE = 30000;

  @Value("${application.network}")
  private String network;

  @Override
  public void updateTokenInfoList() {

    final String latestTokenBalanceCheckpoint =
        getRedisKey(RedisKey.LATEST_TOKEN_BALANCE_CHECKPOINT.name());
    Optional<Block> latestBlock = blockRepository.findLatestBlock();

    // This the slot for the current time
    Long currentSlot = cardanoConverters.time().toSlot(LocalDateTime.now(ZoneOffset.UTC));
    log.info("TokenInfo Scheduler Job: Current slot: {}", currentSlot);
    // Get start time from genesis file
    Long startSlot =
        cardanoConverters.time().toSlot(cardanoConverters.genesisConfig().getStartTime());
    // Last slot processed
    TokenInfoCheckpoint latestProcessedCheckpoint =
        tokenInfoCheckpointRepository
            .findLatestTokenInfoCheckpoint()
            .orElse(TokenInfoCheckpoint.builder().slot(startSlot).build());
    Long latestProcessedSlot = latestProcessedCheckpoint.getSlot();
    Long endSlot = latestProcessedSlot + NUM_SLOT_INTERVAL;

    var maxLedgerSyncAggregationSlot =
        redisTemplate
            .opsForValue()
            .get(
                latestTokenBalanceCheckpoint); // the max slot processed by ledger sync aggregation.
    if (Objects.isNull(maxLedgerSyncAggregationSlot) || latestBlock.isEmpty()) {
      log.error("No block found in the ledger sync database");
      return;
    }

    // the max slot processed by ledger sync main app.
    Long maxLedgerSyncSlot = latestBlock.get().getSlotNo();
    // This is the min block across all the data, as main app or aggregation could be behind
    // We want to be sure all the data used for computation are consistent.
    long maxSafeProcessableSlot =
        Stream.of(endSlot, maxLedgerSyncSlot, maxLedgerSyncAggregationSlot.longValue())
            .sorted()
            .findFirst()
            .get();

    LocalDateTime maxSafeProcessableTime =
        cardanoConverters.slot().slotToTime(maxSafeProcessableSlot);

    Long previousValuesSafeProcessableSlot = maxSafeProcessableSlot;

    // Check if the order of maxTime and now matter (could be positive/negative)
    while (ChronoUnit.MINUTES.between(maxSafeProcessableTime, LocalDateTime.now(ZoneOffset.UTC))
        > 60L) {
      // As long as the upper bound of our time interval is older than 1h, we can safely while loop
      // and
      // not care about rollbacks.

      TokenInfoCheckpoint slotCheckpoint =
          selfProxyService.processTokenInSlotRange(
              latestProcessedSlot,
              maxSafeProcessableSlot,
              currentSlot); // one iteration done and committed on DB, compute
      // next range of slots and continue if while condition still valid

      latestProcessedSlot = slotCheckpoint.getSlot();
      endSlot = latestProcessedSlot + NUM_SLOT_INTERVAL;

      // This is the min block across all the data, as main app or aggregation could be behind
      // We want to be sure all the data used for computation are consistent.
      maxSafeProcessableSlot =
          Stream.of(endSlot, maxLedgerSyncSlot, maxLedgerSyncAggregationSlot.longValue())
              .sorted()
              .findFirst()
              .get();

      if (previousValuesSafeProcessableSlot.equals(maxSafeProcessableSlot)) {
        log.error("The token info scheduler is stuck in a loop, exiting");
        break;
      } else {
        previousValuesSafeProcessableSlot = maxSafeProcessableSlot;
      }

      maxSafeProcessableTime = cardanoConverters.slot().slotToTime(maxSafeProcessableSlot);
      currentSlot = cardanoConverters.time().toSlot(LocalDateTime.now(ZoneOffset.UTC));
    }

    // if we get here, the while condition failed, it means we are in incremental mode, so we must
    // to the same as before, BUT
    // Compute latest aggregation as well as rollback values

    log.info("The token info scheduler switched to incremental mode");
    selfProxyService.processTokenFromSafetySlot(latestProcessedSlot, currentSlot);
    saveTotalTokenCount();
    log.info(
        "Token info scheduler finished processing. The data had processed up to slot: {}",
        currentSlot);
  }

  // We need requires new so that just this block of code is run in isolation
  @Transactional(value = "explorerTransactionManager", propagation = Propagation.REQUIRES_NEW)
  protected TokenInfoCheckpoint processTokenInSlotRange(
      Long fromSlot, Long toSlot, Long currentSlot) {

    TokenInfoCheckpoint checkpoint =
        TokenInfoCheckpoint.builder()
            .slot(toSlot)
            .updateTime(Timestamp.valueOf(LocalDateTime.now()))
            .build();
    long start = System.currentTimeMillis();
    List<String> unitMultiAssetCollection =
        addressTxAmountRepository.getTokensInTransactionInSlotRange(fromSlot, toSlot);
    log.info(
        "Processing token info from slot: {} to slot: {}, with number of units in transaction is {}",
        fromSlot,
        toSlot,
        unitMultiAssetCollection.size());

    if (CollectionUtils.isEmpty(unitMultiAssetCollection)) {
      return checkpoint;
    }

    BatchUtils.doBatching(
        DEFAULT_BATCH_SIZE,
        unitMultiAssetCollection,
        unitsBatch -> {
          List<TokenInfo> tokenInfoList =
              tokenInfoServiceAsync.buildTokenInfoList(unitsBatch, fromSlot, toSlot, currentSlot);
          saveTokenInfoListToDbInRollbackCaseMightNotOccur(tokenInfoList, unitsBatch, toSlot);
        });

    tokenInfoCheckpointRepository.save(checkpoint); // new checkpoint

    log.info(
        "Token info processing from slot: {} to slot: {} took: {} ms",
        fromSlot,
        toSlot,
        System.currentTimeMillis() - start);
    return checkpoint;
  }

  // A safety slot is a time slot that is guaranteed to be in the past, usually equal to the tip
  // minus 24 hours.
  @Transactional(value = "explorerTransactionManager", propagation = Propagation.REQUIRES_NEW)
  protected void processTokenFromSafetySlot(Long latestProcessedSlot, Long tip) {
    List<String> unitMultiAssetCollection =
        addressTxAmountRepository.getTokensInTransactionInSlotRange(latestProcessedSlot, tip);

    if (CollectionUtils.isEmpty(unitMultiAssetCollection)) {
      return;
    }

    // to leverage the async capabilities of the service
    // public CompletableFuture<List<TokenInfo>> buildTokenInfoList(
    //      Set<String> units, Long fromSlot, Long toSlot, Long currentSlot)
    // the toSlot must be the tip because we need calculate from the latest processed slot to the
    // tip

    BatchUtils.doBatching(
        DEFAULT_BATCH_SIZE,
        unitMultiAssetCollection,
        unitsBatch -> {
          List<TokenInfo> tokenInfoList =
              tokenInfoServiceAsync.buildTokenInfoList(unitsBatch, latestProcessedSlot, tip, tip);
          saveTokenInfoListToDbInRollbackCaseMightOccur(tokenInfoList, unitsBatch);
        });
  }

  // The function is used in cases where data is saved to the database and a rollback might not
  // occur.
  // Init Mode
  // Note that:
  // - All values had been computed and the isCalculatedInIncrementalMode fields are set to FALSE
  // - The updated values and previous values always can be TRUSTED
  private void saveTokenInfoListToDbInRollbackCaseMightNotOccur(
      List<TokenInfo> tokenInfoList, List<String> unitMultiAssetCollection, Long toSlot) {

    Map<String, TokenInfo> tokenInfoMap =
        tokenInfoRepository.findByUnitIn(unitMultiAssetCollection).stream()
            .collect(Collectors.toMap(TokenInfo::getUnit, Function.identity()));

    tokenInfoList.forEach(
        tokenInfo -> {
          // If the token info is already in the database
          if (tokenInfoMap.containsKey(tokenInfo.getUnit())) {
            TokenInfo existingTokenInfo = tokenInfoMap.get(tokenInfo.getUnit());
            if (existingTokenInfo.getIsCalculatedInIncrementalMode()
                && Objects.isNull(existingTokenInfo.getPreviousSlot())) {
              // case: the token info had been calculated in incremental mode and the previous
              // values are null
              // meaning: the token info is calculated for the first time in the incremental mode
              // updated values = previous values = values calculated within that slot
              tokenInfo.setPreviousTotalVolume(tokenInfo.getTotalVolume());
              tokenInfo.setPreviousNumberOfHolders(tokenInfo.getNumberOfHolders());
              tokenInfo.setPreviousTotalVolume(tokenInfo.getTotalVolume());
              tokenInfo.setPreviousSlot(tokenInfo.getUpdatedSlot());
            } else if (existingTokenInfo.getIsCalculatedInIncrementalMode()
                && Objects.nonNull(existingTokenInfo.getPreviousSlot())) {
              // The token info had been calculated before in the incremental mode.
              // In this case, the updated values can be untrusted, but the previous values can be
              // trusted
              // then: recalculated the updated values base on the previous values.
              // updated values = previous values + values calculated within that slot
              // previous values unchanged
              tokenInfo.setTotalVolume(
                  existingTokenInfo.getPreviousTotalVolume().add(tokenInfo.getTotalVolume()));
              tokenInfo.setPreviousSlot(existingTokenInfo.getPreviousSlot());
              tokenInfo.setPreviousNumberOfHolders(existingTokenInfo.getPreviousNumberOfHolders());
              tokenInfo.setPreviousTotalVolume(existingTokenInfo.getPreviousTotalVolume());
            } else if (!existingTokenInfo.getIsCalculatedInIncrementalMode()
                && Objects.nonNull(existingTokenInfo.getPreviousSlot())) {
              // The token info had been calculated before in the init mode, then the updated values
              // is recalculated, and its
              // value will be equal the current value plus the value calculated within that slot
              // the previous values are the same as the updated values
              // updated values = current updated values + values calculated within that slot
              // previous values = current updated values
              tokenInfo.setPreviousSlot(existingTokenInfo.getUpdatedSlot());
              tokenInfo.setPreviousTotalVolume(existingTokenInfo.getTotalVolume());
              tokenInfo.setPreviousNumberOfHolders(existingTokenInfo.getNumberOfHolders());
              tokenInfo.setTotalVolume(
                  existingTokenInfo.getTotalVolume().add(tokenInfo.getTotalVolume()));
            }
          } else {
            // the token info is not in the database
            // In the init mode, the token info is calculated for the first time, the updated values
            // and
            // previous values are the same
            tokenInfo.setPreviousTotalVolume(tokenInfo.getTotalVolume());
            tokenInfo.setPreviousNumberOfHolders(tokenInfo.getNumberOfHolders());
            tokenInfo.setPreviousTotalVolume(tokenInfo.getTotalVolume());
            tokenInfo.setPreviousSlot(tokenInfo.getUpdatedSlot());
          }
          tokenInfo.setIsCalculatedInIncrementalMode(false);
        });
    tokenInfoRepository.saveAllAndFlush(tokenInfoList);
  }

  // The function is used in cases where data is saved to the database and a rollback might occur.
  // Incremental Mode
  // Note that:
  // - After updating the token info, the isCalculatedInIncrementalMode fields are set to TRUE
  // - the updated values can be UNTRUSTED, but the previous values always can be TRUSTED
  private void saveTokenInfoListToDbInRollbackCaseMightOccur(
      List<TokenInfo> tokenInfoList, List<String> unitMultiAssetCollection) {
    Map<String, TokenInfo> tokenInfoMap =
        tokenInfoRepository.findByUnitIn(unitMultiAssetCollection).stream()
            .collect(Collectors.toMap(TokenInfo::getUnit, Function.identity()));
    tokenInfoList.forEach(
        tokenInfo -> {
          // if the token is already in the database
          if (tokenInfoMap.containsKey(tokenInfo.getUnit())) {
            TokenInfo existingTokenInfo = tokenInfoMap.get(tokenInfo.getUnit());
            if (existingTokenInfo.getIsCalculatedInIncrementalMode()
                && Objects.nonNull(existingTokenInfo.getPreviousSlot())) {
              // The token info has been calculated in the incremental mode before.
              // In this case, the updated values must not be trusted, but the previous values can
              // be trusted
              // then:
              // updated values = previous values + values calculated within that slot
              // previous values unchanged
              tokenInfo.setTotalVolume(
                  existingTokenInfo.getPreviousTotalVolume().add(tokenInfo.getTotalVolume()));
              tokenInfo.setPreviousSlot(existingTokenInfo.getPreviousSlot());
              tokenInfo.setPreviousTotalVolume(existingTokenInfo.getPreviousTotalVolume());
              tokenInfo.setPreviousNumberOfHolders(existingTokenInfo.getPreviousNumberOfHolders());
              tokenInfo.setPreviousVolume24h(existingTokenInfo.getPreviousVolume24h());
            } else if (!existingTokenInfo.getIsCalculatedInIncrementalMode()) {
              // The token info has been calculated before in the init mode.
              // In this case, the updated values must be recalculated, and its value will be equal
              // the current value plus the value calculated within that slot
              // the previous values are the same as the updated values
              tokenInfo.setPreviousSlot(existingTokenInfo.getUpdatedSlot());
              tokenInfo.setPreviousTotalVolume(existingTokenInfo.getTotalVolume());
              tokenInfo.setPreviousNumberOfHolders(existingTokenInfo.getNumberOfHolders());
              tokenInfo.setTotalVolume(
                  existingTokenInfo.getTotalVolume().add(tokenInfo.getTotalVolume()));
            }
          }
          tokenInfo.setIsCalculatedInIncrementalMode(true);
        });
    // if the condition is false, the token info is not in the database
    // then: the updated values equal the values calculated within that slot and the previous values
    // must be null
    tokenInfoRepository.saveAllAndFlush(tokenInfoList);
  }

  /** Save total token count into redis cache. */
  void saveTotalTokenCount() {
    String redisKey = getRedisKey(AGGREGATED_CACHE.name());
    long totalTokenCount = multiAssetRepository.count();
    redisTemplate
        .opsForHash()
        .put(redisKey, TOTAL_TOKEN_COUNT.name(), String.valueOf(totalTokenCount));
    log.info("Total token count: {}", totalTokenCount);
  }

  private String getRedisKey(String prefix) {
    return prefix + "_" + network;
  }
}
