package org.cardanofoundation.job.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.explorer.common.entity.explorer.TokenInfo;
import org.cardanofoundation.explorer.common.entity.explorer.TokenInfoCheckpoint;
import org.cardanofoundation.explorer.common.entity.ledgersync.Block;
import org.cardanofoundation.explorer.common.entity.ledgersync.MultiAsset;
import org.cardanofoundation.job.model.TokenTxCount;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.repository.explorer.TokenInfoCheckpointRepository;
import org.cardanofoundation.job.repository.explorer.TokenInfoRepository;
import org.cardanofoundation.job.repository.explorer.jooq.JOOQTokenInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.AddressTxAmountRepository;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.MultiAssetRepository;
import org.cardanofoundation.job.repository.ledgersync.TxRepository;
import org.cardanofoundation.job.service.MultiAssetService;
import org.cardanofoundation.job.service.TokenInfoService;
import org.cardanofoundation.job.service.TokenInfoServiceAsync;
import org.cardanofoundation.job.util.BatchUtils;
import org.cardanofoundation.job.util.StreamUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.cardanofoundation.job.common.enumeration.RedisKey.AGGREGATED_CACHE;
import static org.cardanofoundation.job.common.enumeration.RedisKey.TOTAL_TOKEN_COUNT;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenInfoServiceImpl implements TokenInfoService {

  private final TokenInfoCheckpointRepository tokenInfoCheckpointRepository;
  private final BlockRepository blockRepository;
  private final MultiAssetRepository multiAssetRepository;
  private final TokenInfoRepository tokenInfoRepository;
  private final TokenInfoServiceAsync tokenInfoServiceAsync;
  private final TxRepository txRepository;
  private final JOOQTokenInfoRepository jooqTokenInfoRepository;
  private final AddressTxAmountRepository addressTxAmountRepository;
  private final MultiAssetService multiAssetService;

  private final RedisTemplate<String, String> redisTemplate;

  @Value("${application.network}")
  private String network;

  @Value("${jobs.token-info.token-info-batch-size:1000}")
  private Integer tokenInfoBatchSize;

  @Override
  @Transactional(value = "explorerTransactionManager")
  @SneakyThrows
  public void updateTokenInfoList() {
    Optional<Block> latestBlock = blockRepository.findLatestBlock();
    if (latestBlock.isEmpty()) {
      return;
    }
    Long maxBlockNo = latestBlock.get().getBlockNo();
    Timestamp timeLatestBlock = latestBlock.get().getTime();
    var tokenInfoCheckpoint = tokenInfoCheckpointRepository.findLatestTokenInfoCheckpoint();

    if (tokenInfoCheckpoint.isEmpty()) {
      // If no token info checkpoint found, this means it's the first update,
      // so initialize token info data for the first time.
      initializeTokenInfoDataForFirstTime(maxBlockNo, timeLatestBlock);
    } else {
      // If a checkpoint exists, update the existing token info data by
      // inserting new data and updating existing ones.
      updateExistingTokenInfo(tokenInfoCheckpoint.get(), maxBlockNo, timeLatestBlock);
    }

    // Save total token count into redis cache
    saveTotalTokenCount();
  }

  /**
   * Initialize token info data for the first time.
   *
   * @param maxBlockNo The maximum block number.
   * @param updateTime The update time.
   */
  private void initializeTokenInfoDataForFirstTime(Long maxBlockNo, Timestamp updateTime) {
    log.info("Init token info data for the first time");

    var maxMultiAssetId = multiAssetRepository.getCurrentMaxIdent();

    if (maxMultiAssetId == null) {
      log.info("No multi-asset found >>> Skip token info initialization");
      return;
    }

    Timestamp time24hAgo = Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC).minusDays(1));

    // Find the minimum transaction ID that occurred after the time 24 hours ago.
    Long txId = txRepository.findMinTxByAfterTime(time24hAgo).orElse(Long.MAX_VALUE);

    // Define the maximum batch size for processing multi-assets.
      var numBatches = maxMultiAssetId / tokenInfoBatchSize;
      var reminder = maxMultiAssetId % tokenInfoBatchSize;

      log.info("numBatches: {}", numBatches);
      log.info("reminder: {}", reminder);

    // Process the multi-assets in batches to build token info data.
    for (int i = 0; i < numBatches; i++) {

        var startIndex = (long) i * tokenInfoBatchSize + 1;
        var toIndex = startIndex + tokenInfoBatchSize;
        if (i == numBatches -1) {
            toIndex += reminder;
        }

        if (i % 5 == 0) {
            log.info("TokenInfoBatch - index: {}, start: {}, end: {}", i, startIndex, toIndex);
        }


          var tokenInfoList = tokenInfoServiceAsync
              .buildTokenInfoList(startIndex, toIndex, maxBlockNo, txId, updateTime)
              .exceptionally(
                  e -> {
                      log.warn("TokenInfoBatch error while executing batch", e);
                    throw new RuntimeException(
                        "Exception occurs when initializing token info list", e);
                  })
                  .join();
        jooqTokenInfoRepository.insertAll(tokenInfoList);

    }

    tokenInfoCheckpointRepository.save(
        TokenInfoCheckpoint.builder().blockNo(maxBlockNo).updateTime(updateTime).build());
  }

  /**
   * Update existing token info by inserting new data and updating existing ones.
   *
   * @param tokenInfoCheckpoint The latest token info checkpoint.
   * @param maxBlockNo The maximum block number.
   * @param updateTime The update time.
   */
  private void updateExistingTokenInfo(
      TokenInfoCheckpoint tokenInfoCheckpoint, Long maxBlockNo, Timestamp updateTime) {
    if (maxBlockNo.equals(tokenInfoCheckpoint.getBlockNo())) {
      return;
    }

    Timestamp time24hAgo = Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
    var lastUpdateTime = tokenInfoCheckpoint.getUpdateTime();

    Long txId = txRepository.findMinTxByAfterTime(time24hAgo).orElse(Long.MAX_VALUE);

    // Retrieve multi-assets involved in transactions between the last processed block and the
    // latest block.
    var tokensInTransactionWithNewBlockRange =
        multiAssetRepository.getTokensInTransactionInBlockRange(
            tokenInfoCheckpoint.getBlockNo(), maxBlockNo);
    log.info(
        "tokensInTransactionWithNewBlockRange has size: {}",
        tokensInTransactionWithNewBlockRange.size());
    var tokensInTransactionWithNewBlockRangeMap =
        StreamUtil.toMap(
            tokensInTransactionWithNewBlockRange, MultiAsset::getId, Function.identity());

    // Retrieve multi-assets involved in transactions
    // from 24h before last update time to 24h before the current time
    var tokenNeedUpdateVolume24h =
        multiAssetRepository.getTokensInTransactionInTimeRange(
            Timestamp.valueOf(lastUpdateTime.toLocalDateTime().minusDays(1)), time24hAgo);

    log.info("tokenNeedUpdateVolume24h has size: {}", tokenNeedUpdateVolume24h.size());
    var tokenNeedUpdateVolume24hMap =
        StreamUtil.toMap(tokenNeedUpdateVolume24h, MultiAsset::getId, Function.identity());

    // Create a map that merges all the multi-assets that need to be processed in this update.
    var tokenToProcessMap = new HashMap<Long, MultiAsset>();
    tokenToProcessMap.putAll(tokensInTransactionWithNewBlockRangeMap);
    tokenToProcessMap.putAll(tokenNeedUpdateVolume24hMap);

    log.info("tokenToProcess has size: {}", tokenToProcessMap.size());

    // Process the tokens to update the corresponding TokenInfo entities in batches of 10,000.
    BatchUtils.doBatching(
        10000,
        tokenToProcessMap.values().stream().toList(),
        multiAssets -> {
          // Create a list of multi-asset IDs to process in this batch.
          List<Long> multiAssetIds = StreamUtil.mapApply(multiAssets, MultiAsset::getId);
          List<TokenInfo> saveEntities = new ArrayList<>();
          List<TokenVolume> volumes =
              addressTxAmountRepository.sumBalanceAfterTx(multiAssetIds, txId);
          var tokenVolume24hMap =
              StreamUtil.toMap(volumes, TokenVolume::getIdent, TokenVolume::getVolume);
          var totalVolumeMap =
              addressTxAmountRepository.getTotalVolumeByIdentIn(multiAssetIds).stream()
                  .collect(Collectors.toMap(TokenVolume::getIdent, TokenVolume::getVolume));

          var mapNumberHolder = multiAssetService.getMapNumberHolder(multiAssetIds);
          var totalTxCountMap =
              addressTxAmountRepository.getTotalTxCountByIdentIn(multiAssetIds).stream()
                  .collect(Collectors.toMap(TokenTxCount::getIdent, TokenTxCount::getTxCount));

          var tokenInfoMap =
              tokenInfoRepository.findByMultiAssetIdIn(multiAssetIds).stream()
                  .collect(Collectors.toMap(TokenInfo::getMultiAssetId, Function.identity()));

          multiAssetIds.forEach(
              multiAssetId -> {
                var tokenInfo = tokenInfoMap.getOrDefault(multiAssetId, new TokenInfo());
                tokenInfo.setMultiAssetId(multiAssetId);
                tokenInfo.setVolume24h(
                    tokenVolume24hMap.getOrDefault(multiAssetId, BigInteger.ZERO));
                tokenInfo.setNumberOfHolders(mapNumberHolder.getOrDefault(multiAssetId, 0L));
                tokenInfo.setTotalVolume(
                    totalVolumeMap.getOrDefault(multiAssetId, BigInteger.ZERO));
                tokenInfo.setTxCount(totalTxCountMap.getOrDefault(multiAssetId, 0L));
                tokenInfo.setUpdateTime(updateTime);
                tokenInfo.setBlockNo(maxBlockNo);
                saveEntities.add(tokenInfo);
              });

          BatchUtils.doBatching(500, saveEntities, tokenInfoRepository::saveAll);
        });

    tokenInfoCheckpointRepository.save(
        TokenInfoCheckpoint.builder().blockNo(maxBlockNo).updateTime(updateTime).build());
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


    @PostConstruct
    public void init(){
        log.info("INIT - tokenInfoBatchSize: {}", tokenInfoBatchSize);
    }

}
