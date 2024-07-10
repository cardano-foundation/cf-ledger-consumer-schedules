package org.cardanofoundation.job.service.impl;

import static org.cardanofoundation.job.common.enumeration.RedisKey.AGGREGATED_CACHE;
import static org.cardanofoundation.job.common.enumeration.RedisKey.TOTAL_TOKEN_COUNT;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Iterables;

import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.conversions.ClasspathConversionsFactory;
import org.cardanofoundation.conversions.domain.NetworkType;
import org.cardanofoundation.explorer.common.entity.explorer.TokenInfo;
import org.cardanofoundation.explorer.common.entity.explorer.TokenInfoCheckpoint;
import org.cardanofoundation.job.projection.TokenUnitProjection;
import org.cardanofoundation.job.repository.explorer.TokenInfoCheckpointRepository;
import org.cardanofoundation.job.repository.explorer.TokenInfoRepository;
import org.cardanofoundation.job.repository.explorer.jooq.JOOQTokenInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.*;
import org.cardanofoundation.job.service.MultiAssetService;
import org.cardanofoundation.job.service.TokenInfoService;
import org.cardanofoundation.job.service.TokenInfoServiceAsync;
import org.cardanofoundation.job.util.BatchUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenInfoServiceImpl implements TokenInfoService {

  private final TokenInfoCheckpointRepository tokenInfoCheckpointRepository;
  private final BlockRepository blockRepository;
  private final MultiAssetRepository multiAssetRepository;
  private final TokenInfoRepository tokenInfoRepository;
  private final TokenInfoServiceAsync tokenInfoServiceAsync;
  private final JOOQTokenInfoRepository jooqTokenInfoRepository;
  private final AddressTxAmountRepository addressTxAmountRepository;
  private final MultiAssetService multiAssetService;
  private final LatestTokenBalanceRepository latestTokenBalanceRepository;
  private final AddressBalanceRepository addressBalanceRepository;

  private final RedisTemplate<String, String> redisTemplate;

  CardanoConverters converters = ClasspathConversionsFactory.createConverters(NetworkType.MAINNET);

  @Value("${application.network}")
  private String network;

  @Value("${jobs.token-info.batch-size}")
  private int tokenInfoBatchSize;

  @Value("${jobs.token-info.block-interval-size}")
  private int tokenInfoBlockIntervalSize;

  @Override
  @Transactional(value = "explorerTransactionManager")
  @SneakyThrows
  public void updateTokenInfoList() {
    var latestBlock = blockRepository.findLatestBlock();
    var addressBalance = addressBalanceRepository.findFirstBy(Sort.by("blockNumber").descending());
    log.info("latestBlock: {}", latestBlock);
    log.info("addressBalance: {}", addressBalance);

    if (latestBlock.isEmpty() || addressBalance.isEmpty()) {
      return;
    }

    Long maxBlockNoFromLsAgg = addressBalance.get().getBlockTime();
    Long latestBlockNo = Math.min(maxBlockNoFromLsAgg, latestBlock.get().getBlockNo());

    log.info(
        "Compare latest block no from LS_AGG: {} and latest block no from LS_MAIN: {}",
        maxBlockNoFromLsAgg,
        latestBlock.get().getBlockNo());

    Timestamp timeLatestBlock = blockRepository.getBlockTimeByBlockNo(latestBlockNo);
    var tokenInfoCheckpoint = tokenInfoCheckpointRepository.findLatestTokenInfoCheckpoint();

    if (tokenInfoCheckpoint.isEmpty()) {
      // If no token info checkpoint found, this means it's the first update,
      // so initialize token info data for the first time.
      initializeTokenInfoDataForFirstTime(latestBlockNo, timeLatestBlock);
    } else {

      Long blockNumberTo;
      Timestamp timeBlockTo;
      if (latestBlockNo - tokenInfoCheckpoint.get().getBlockNo() > tokenInfoBlockIntervalSize) {
        blockNumberTo = tokenInfoCheckpoint.get().getBlockNo() + tokenInfoBlockIntervalSize;
        timeBlockTo = blockRepository.getBlockTimeByBlockNo(blockNumberTo);
      } else {
        blockNumberTo = latestBlockNo;
        timeBlockTo = timeLatestBlock;
      }

      // If a checkpoint exists, update the existing token info data by
      // inserting new data and updating existing ones.
      updateExistingTokenInfo(
          tokenInfoCheckpoint.get(), blockNumberTo, timeBlockTo, timeLatestBlock);
    }

    // Save total token count into redis cache
    saveTotalTokenCount();
  }

  /**
   * Initialize token info data for the first time.
   *
   * @param maxBlockNo The maximum block number.
   * @param timeLatestBlock The update time.
   */
  private void initializeTokenInfoDataForFirstTime(Long maxBlockNo, Timestamp timeLatestBlock) {
    log.info("Init token info data for the first time");

    List<CompletableFuture<List<TokenInfo>>> tokenInfoFutures = new ArrayList<>();

    var maxMultiAssetId = multiAssetRepository.getCurrentMaxIdent();

    if (maxMultiAssetId == null) {
      log.info("No multi-asset found >>> Skip token info initialization");
      return;
    }

    // Collect the results from all CompletableFuture instances to get the list of multi-assets.
    var multiAssetIdList =
        LongStream.rangeClosed(1, maxMultiAssetId).boxed().collect(Collectors.toList());

    Long epochSecond24hAgo =
        LocalDateTime.now(ZoneOffset.UTC).minusDays(1).toEpochSecond(ZoneOffset.UTC);

    // Define the maximum batch size for processing multi-assets.
    int multiAssetListSize = tokenInfoBatchSize;

    // Process the multi-assets in batches to build token info data.
    for (int i = 0; i < multiAssetIdList.size(); i += multiAssetListSize) {
      int toIndex = Math.min(i + multiAssetListSize, multiAssetIdList.size());
      var subList = multiAssetIdList.subList(i, toIndex);
      Long startIdent = subList.get(0);
      Long endIdent = subList.get(subList.size() - 1);

      tokenInfoFutures.add(
          tokenInfoServiceAsync
              .buildTokenInfoList(
                  startIdent, endIdent, maxBlockNo, epochSecond24hAgo, timeLatestBlock)
              .exceptionally(
                  e -> {
                    throw new RuntimeException(
                        "Exception occurs when initializing token info list", e);
                  }));

      // After every 5 batches, insert the fetched token info data into the database in batches.
      if (tokenInfoFutures.size() % 5 == 0) {
        var tokenInfoList =
            tokenInfoFutures.stream().map(CompletableFuture::join).flatMap(List::stream).toList();
        BatchUtils.doBatching(1000, tokenInfoList, jooqTokenInfoRepository::insertAll);
        tokenInfoFutures.clear();
      }
    }

    // Wait for the remaining CompletableFuture instances to complete.
    CompletableFuture.allOf(tokenInfoFutures.toArray(new CompletableFuture[0])).join();
    List<TokenInfo> tokenInfoList =
        tokenInfoFutures.stream().map(CompletableFuture::join).flatMap(List::stream).toList();
    BatchUtils.doBatching(1000, tokenInfoList, jooqTokenInfoRepository::insertAll);

    multiAssetIdList.clear();

    tokenInfoCheckpointRepository.save(
        TokenInfoCheckpoint.builder().blockNo(maxBlockNo).updateTime(timeLatestBlock).build());
  }

  /**
   * Update existing token info by inserting new data and updating existing ones.
   *
   * @param tokenInfoCheckpoint The latest token info checkpoint.
   * @param maxBlockNo The maximum block number.
   * @param timeLatestProcessableBlock is the time of the latest block for which we have all stats
   * @param timeLatestBlock The update time.
   */
  private void updateExistingTokenInfo(
      TokenInfoCheckpoint tokenInfoCheckpoint,
      Long maxBlockNo,
      Timestamp timeLatestBlock,
      Timestamp timeLatestProcessableBlock) {
    if (maxBlockNo.compareTo(tokenInfoCheckpoint.getBlockNo()) <= 0) {
      log.info(
          "Stop updating token info as the latest block no is not greater than the checkpoint, {} <= {}",
          maxBlockNo,
          tokenInfoCheckpoint.getBlockNo());
      return;
    }
    log.info(
        "Update existing token info from blockNo: {} to blockNo: {}",
        tokenInfoCheckpoint.getBlockNo(),
        maxBlockNo);

    var txSlotFrom =
        converters.time().toSlot(tokenInfoCheckpoint.getUpdateTime().toLocalDateTime());
    var txSlotTo = converters.time().toSlot(timeLatestBlock.toLocalDateTime());

    log.info("slot range, from: {}, to: {}", txSlotFrom, txSlotTo);

    // Retrieve multi-assets involved in transactions between the last processed block and the
    // latest block.
    //    List<String> tokensInTransactionWithNewBlockRange =
    //        addressTxAmountRepository.getTokensInTransactionInTimeRange(
    //            tokenInfoCheckpoint.getUpdateTime().toInstant().getEpochSecond(),
    //            timeLatestBlock.toInstant().getEpochSecond());

    List<String> tokensInTransactionWithNewBlockRange =
        addressTxAmountRepository.getTokensInTransactionInSlotRange(txSlotFrom, txSlotTo);

    log.info(
        "tokensInTransactionWithNewBlockRange has size: {}",
        tokensInTransactionWithNewBlockRange.size());

    //    Long epochSecondLastUpdateTime =
    //        tokenInfoCheckpoint
    //            .getUpdateTime()
    //            .toLocalDateTime()
    //            .minusDays(1)
    //            .toEpochSecond(ZoneOffset.UTC);

    LocalDateTime epochSecond24hAgo = LocalDateTime.now(ZoneOffset.UTC).minusDays(1);

    // Retrieve multi-assets involved in transactions
    // from 24h before last update time to 24h before the current time
    //    List<String> tokenNeedUpdateVolume24h =
    //        addressTxAmountRepository.getTokensInTransactionInTimeRange(
    //            epochSecondLastUpdateTime, epochSecond24hAgo);

    //    var recentTxSlotFrom =
    //        converters
    //            .time()
    //            .toSlot(tokenInfoCheckpoint.getUpdateTime().toLocalDateTime().minusDays(1));
    //    var recentTxSlotTo =
    // converters.time().toSlot(LocalDateTime.now(ZoneOffset.UTC).minusDays(1));

    //    List<String> tokenNeedUpdateVolume24h =
    //        addressTxAmountRepository.getTokensInTransactionInSlotRange(
    //            recentTxSlotFrom, recentTxSlotTo);

    // Create a map that merges all the multi-assets that need to be processed in this update.
    Set<String> tokenToProcessSet = new HashSet<>();
    tokenToProcessSet.addAll(tokensInTransactionWithNewBlockRange);
    //    tokenToProcessSet.addAll(tokenNeedUpdateVolume24h);

    log.info("tokenToProcess has size: {}", tokenToProcessSet.size());

    final var counter = new AtomicLong(0L);
    List<String> filteredTokenToProcess = new ArrayList<>();
    Iterables.partition(tokenToProcessSet, 50)
        .forEach(
            units -> {
              var tokenUnitIdents = multiAssetRepository.getTokenUnitByUnitIn(units);
              var tokenInfoIdents =
                  tokenUnitIdents.stream().map(TokenUnitProjection::getIdent).toList();
              var tokenInfos = tokenInfoRepository.findByMultiAssetIdIn(tokenInfoIdents);
              tokenInfos.forEach(
                  tokenInfo -> {
                    if (tokenInfo.getUpdateTime().before(tokenInfoCheckpoint.getUpdateTime())) {
                      var tokenToProcessOpt =
                          tokenUnitIdents.stream()
                              .filter(
                                  tokenIdent ->
                                      tokenIdent.getIdent().equals(tokenInfo.getMultiAssetId()))
                              .findFirst();

                      tokenToProcessOpt.ifPresent(
                          tokenIdent -> filteredTokenToProcess.add(tokenIdent.getUnit()));

                      if (tokenToProcessOpt.isEmpty()) {
                        log.warn("expected token to process, could not be found.");
                      }
                    } else {
                      counter.incrementAndGet();
                    }
                  });
            });

    log.info("skipped {} tokens refresh", counter.get());

    filteredTokenToProcess.forEach(
        unit -> {
          var startTime = System.currentTimeMillis();
          var tokenInfoList =
              tokenInfoServiceAsync.buildTokenInfoList(
                  List.of(unit), maxBlockNo, epochSecond24hAgo, timeLatestProcessableBlock);
          log.info("buildTokenInfoList elapsed: {}ms", System.currentTimeMillis() - startTime);

          BatchUtils.doBatching(1000, tokenInfoList, jooqTokenInfoRepository::insertAll);
        });

    //    tokenToProcessSet.forEach(
    //        unit -> {
    //          var startTime = System.currentTimeMillis();
    //          var tokenInfoList =
    //              tokenInfoServiceAsync.buildTokenInfoList(
    //                  List.of(unit), maxBlockNo, epochSecond24hAgo, timeLatestBlock);
    //          log.info("buildTokenInfoList elapsed: {}ms", System.currentTimeMillis() -
    // startTime);
    //
    //          BatchUtils.doBatching(1000, tokenInfoList, jooqTokenInfoRepository::insertAll);
    //        });

    // Process the tokens to update the corresponding TokenInfo entities in batches of 10,000.
    //    BatchUtils.doBatching(
    //        1000,
    //        tokenToProcessSet.stream().toList(),
    //        units -> {
    //          var tokenInfoList =
    //              tokenInfoServiceAsync.buildTokenInfoList(
    //                  units, maxBlockNo, epochSecond24hAgo, timeLatestBlock);
    //
    //          BatchUtils.doBatching(1000, tokenInfoList, jooqTokenInfoRepository::insertAll);
    //        });

    tokenInfoCheckpointRepository.save(
        TokenInfoCheckpoint.builder().blockNo(maxBlockNo).updateTime(timeLatestBlock).build());
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
