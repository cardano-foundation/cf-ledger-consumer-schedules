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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.explorer.common.entity.explorer.TokenInfo;
import org.cardanofoundation.explorer.common.entity.explorer.TokenInfoCheckpoint;
import org.cardanofoundation.explorer.common.entity.ledgersync.Block;
import org.cardanofoundation.job.repository.explorer.TokenInfoCheckpointRepository;
import org.cardanofoundation.job.repository.explorer.TokenInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.MultiAssetRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.LatestTokenBalanceRepository;
import org.cardanofoundation.job.service.TokenInfoService;
import org.cardanofoundation.job.service.TokenInfoServiceAsync;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenInfoServiceImpl implements TokenInfoService {

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

  @Value("${application.network}")
  private String network;

  @Override
  public void updateTokenInfoList() {

    Optional<Block> latestBlock = blockRepository.findLatestBlock();

    // This the slot for the real time
    Long currentSlot = cardanoConverters.time().toSlot(LocalDateTime.now(ZoneOffset.UTC));

    log.info("Current slot: {}", currentSlot);

    // get start time from genesis file
    Long startSlot =
        cardanoConverters.time().toSlot(cardanoConverters.genesisConfig().getStartTime());

    // Last slot processed
    TokenInfoCheckpoint latestProcessedCheckpoint =
        tokenInfoCheckpointRepository
            .findLatestTokenInfoCheckpoint()
            .orElse(TokenInfoCheckpoint.builder().slot(startSlot).build());
    Long latestProcessedSlot = latestProcessedCheckpoint.getSlot();
    Long endSlot = latestProcessedSlot + NUM_SLOT_INTERVAL;

    if (latestBlock.isEmpty()) {
      log.error("No block found in the ledger sync database");
      return;
    }

    Long maxLedgerSyncSlot =
        latestBlock.get().getSlotNo(); // the max slot processed by ledger sync main app.
    Long maxLedgerSyncAggregationSlot =
        latestTokenBalanceRepository
            .getTheSecondLastSlot(); // the max slot processed by ledger sync aggregation.

    if (Objects.isNull(maxLedgerSyncSlot) || Objects.isNull(maxLedgerSyncAggregationSlot)) {
      log.error("No block found in the ledger sync database");
      return;
    }

    // This is the min block across all the data, as main app or aggregation could be behind
    // We want to be sure all the data used for computation are consistent.
    long maxSafeProcessableSlot =
        Stream.of(endSlot, maxLedgerSyncSlot, maxLedgerSyncAggregationSlot)
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
          processTokenInSlotRange(
              latestProcessedSlot,
              maxSafeProcessableSlot,
              currentSlot); // one iteration done and committed on DB, compute
      // next range of slots and continue if while condition still valid

      latestProcessedSlot = slotCheckpoint.getSlot();
      endSlot = latestProcessedSlot + NUM_SLOT_INTERVAL;

      // This is the min block across all the data, as main app or aggregation could be behind
      // We want to be sure all the data used for computation are consistent.
      maxSafeProcessableSlot =
          Stream.of(endSlot, maxLedgerSyncSlot, maxLedgerSyncAggregationSlot)
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
    processTokenFromSafetySlot(latestProcessedSlot, currentSlot);
    saveTotalTokenCount();
    log.info(
        "Token info scheduler finished processing. The data had processed up to slot: {}",
        currentSlot);
  }

  // We need requires new so that just this block of code is run in isolation
  @Transactional(value = "explorerTransactionManager", propagation = Propagation.REQUIRES_NEW)
  public TokenInfoCheckpoint processTokenInSlotRange(Long fromSlot, Long toSlot, Long currentSlot) {

    TokenInfoCheckpoint checkpoint =
        TokenInfoCheckpoint.builder()
            .slot(toSlot)
            .updateTime(Timestamp.valueOf(LocalDateTime.now()))
            .build();
    long start = System.currentTimeMillis();
    Set<String> unitMultiAssetCollection =
        addressTxAmountRepository.getTokensInTransactionInSlotRange(fromSlot, toSlot);
    log.info(
        "Processing token info from slot: {} to slot: {}, with number of units in transaction is {}",
        fromSlot,
        toSlot,
        unitMultiAssetCollection.size());

    if (CollectionUtils.isEmpty(unitMultiAssetCollection)) {
      return checkpoint;
    }

    List<TokenInfo> tokenInfoList =
        tokenInfoServiceAsync.buildTokenInfoList(
            unitMultiAssetCollection, fromSlot, toSlot, currentSlot);

    saveTokenInfoListToDbInRollbackCaseMightNotOccur(
        tokenInfoList, unitMultiAssetCollection, toSlot);

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
  private void processTokenFromSafetySlot(Long latestProcessedSlot, Long tip) {
    Set<String> unitMultiAssetCollection =
        addressTxAmountRepository.getTokensInTransactionInSlotRange(latestProcessedSlot, tip);

    if (CollectionUtils.isEmpty(unitMultiAssetCollection)) {
      return;
    }

    // to leverage the async capabilities of the service
    // public CompletableFuture<List<TokenInfo>> buildTokenInfoList(
    //      Set<String> units, Long fromSlot, Long toSlot, Long currentSlot)
    // the toSlot must be the tip because we need calculate from the latest processed slot to the
    // tip

    List<TokenInfo> tokenInfoList =
        tokenInfoServiceAsync.buildTokenInfoList(
            unitMultiAssetCollection, latestProcessedSlot, tip, tip);

    saveTokenInfoListToDbInRollbackCaseMightOccur(tokenInfoList, unitMultiAssetCollection);
  }

  private void saveTokenInfoListToDbInRollbackCaseMightNotOccur(
      List<TokenInfo> tokenInfoList, Set<String> unitMultiAssetCollection, Long toSlot) {

    Map<String, TokenInfo> tokenInfoMap =
        tokenInfoRepository.findByUnitIn(unitMultiAssetCollection).stream()
            .collect(Collectors.toMap(TokenInfo::getUnit, Function.identity()));

    tokenInfoList.forEach(
        tokenInfo -> {
          if (tokenInfoMap.containsKey(tokenInfo.getUnit())) {
            // if the token is already in the database
            // then update the previous values with the existing values and the updated values with
            // the new values
            TokenInfo existingTokenInfo = tokenInfoMap.get(tokenInfo.getUnit());

            if (Objects.isNull(existingTokenInfo.getPreviousSlot())) {
              tokenInfo.setPreviousNumberOfHolders(tokenInfo.getNumberOfHolders());
              tokenInfo.setPreviousTotalVolume(tokenInfo.getTotalVolume());
              tokenInfo.setPreviousVolume24h(tokenInfo.getVolume24h());
              tokenInfo.setPreviousSlot(tokenInfo.getUpdatedSlot());
            } else {
              // case: The updated slot is greater than the latest processed slot and there is no
              // previous slot
              // meaning: The token is updated in a slot that might be rolled back
              // then: update the updated values and the previous value
              // note that: current slot ( toSlot) is the trusted slot that will not be rolled back
              tokenInfo.setPreviousNumberOfHolders(existingTokenInfo.getNumberOfHolders());
              tokenInfo.setPreviousTotalVolume(existingTokenInfo.getTotalVolume());
              tokenInfo.setPreviousVolume24h(existingTokenInfo.getVolume24h());
              tokenInfo.setPreviousSlot(existingTokenInfo.getUpdatedSlot());
              tokenInfo.setTotalVolume(
                  existingTokenInfo.getTotalVolume().add(tokenInfo.getTotalVolume()));
            }
          } else {
            // if the token is not in the database
            // then the previous values must be equal to the updated values
            tokenInfo.setPreviousNumberOfHolders(tokenInfo.getNumberOfHolders());
            tokenInfo.setPreviousTotalVolume(tokenInfo.getTotalVolume());
            tokenInfo.setPreviousVolume24h(tokenInfo.getVolume24h());
            tokenInfo.setPreviousSlot(tokenInfo.getUpdatedSlot());
          }
        });
    tokenInfoRepository.saveAllAndFlush(tokenInfoList);
  }

  // The function is used in cases where data is saved to the database and a rollback might occur.
  private void saveTokenInfoListToDbInRollbackCaseMightOccur(
      List<TokenInfo> tokenInfoList, Set<String> unitMultiAssetCollection) {
    Map<String, TokenInfo> tokenInfoMap =
        tokenInfoRepository.findByUnitIn(unitMultiAssetCollection).stream()
            .collect(Collectors.toMap(TokenInfo::getUnit, Function.identity()));
    tokenInfoList.forEach(
        tokenInfo -> {
          // if the token is already in the database
          if (tokenInfoMap.containsKey(tokenInfo.getUnit())) {
            TokenInfo existingTokenInfo = tokenInfoMap.get(tokenInfo.getUnit());

            if (Objects.nonNull(existingTokenInfo.getPreviousSlot())) {
              // case: The previous slot is not null
              // then: The updated values must be added to the previous values
              tokenInfo.setPreviousSlot(existingTokenInfo.getUpdatedSlot());
              tokenInfo.setPreviousNumberOfHolders(existingTokenInfo.getNumberOfHolders());
              tokenInfo.setPreviousTotalVolume(existingTokenInfo.getTotalVolume());
              tokenInfo.setPreviousVolume24h(existingTokenInfo.getVolume24h());
              tokenInfo.setTotalVolume(
                  existingTokenInfo.getPreviousTotalVolume().add(tokenInfo.getTotalVolume()));
            }
          }
        });

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

  private String getRedisKey(String key) {
    return String.join("_", network.toUpperCase(), key);
  }
}
