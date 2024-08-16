package org.cardanofoundation.job.service.impl;

import static org.cardanofoundation.job.common.enumeration.RedisKey.AGGREGATED_CACHE;
import static org.cardanofoundation.job.common.enumeration.RedisKey.TOTAL_TOKEN_COUNT;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.explorer.common.entity.explorer.TokenInfoCheckpoint;
import org.cardanofoundation.explorer.common.entity.ledgersync.Block;
import org.cardanofoundation.job.repository.explorer.TokenInfoCheckpointRepository;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.MultiAssetRepository;
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

    // This is the min block across all the data, as main app or aggregation could be behind
    // We want to be sure all the data used for computation are consistent.
    Long maxSafeProcessableSlot =
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
          tokenInfoServiceAsync.processTokenInRange(
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
    tokenInfoServiceAsync.processTokenFromSafetySlot(latestProcessedSlot, currentSlot);
    saveTotalTokenCount();
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
