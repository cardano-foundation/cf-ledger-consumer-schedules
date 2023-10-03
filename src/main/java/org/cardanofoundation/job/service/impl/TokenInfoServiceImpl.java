package org.cardanofoundation.job.service.impl;

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

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.consumercommon.entity.Block;
import org.cardanofoundation.explorer.consumercommon.entity.MultiAsset;
import org.cardanofoundation.explorer.consumercommon.entity.TokenInfo;
import org.cardanofoundation.explorer.consumercommon.entity.TokenInfoCheckpoint;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.repository.BlockRepository;
import org.cardanofoundation.job.repository.MultiAssetRepository;
import org.cardanofoundation.job.repository.TokenInfoCheckpointRepository;
import org.cardanofoundation.job.repository.TokenInfoRepository;
import org.cardanofoundation.job.repository.TxRepository;
import org.cardanofoundation.job.repository.jooq.JOOQAddressTokenRepository;
import org.cardanofoundation.job.repository.jooq.JOOQMultiAssetRepository;
import org.cardanofoundation.job.repository.jooq.JOOQTokenInfoRepository;
import org.cardanofoundation.job.service.MultiAssetDataProcessorService;
import org.cardanofoundation.job.service.TokenInfoService;
import org.cardanofoundation.job.service.TokenInfoServiceAsync;
import org.cardanofoundation.job.util.BatchUtils;
import org.cardanofoundation.job.util.StreamUtil;

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
  private final JOOQMultiAssetRepository jooqMultiAssetRepository;
  private final JOOQAddressTokenRepository jooqAddressTokenRepository;
  private final MultiAssetDataProcessorService multiAssetDataProcessorService;

  @Override
  @Transactional
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
  }

  /**
   * Initialize token info data for the first time.
   *
   * @param maxBlockNo The maximum block number.
   * @param updateTime The update time.
   */
  private void initializeTokenInfoDataForFirstTime(Long maxBlockNo, Timestamp updateTime) {
    log.info("Init token info data for the first time");

    // The sublist size to divide the multi-asset list into smaller batches for processing.
    int subListSize = 10000;

    List<CompletableFuture<List<TokenInfo>>> tokenInfoFutures = new ArrayList<>();
    List<CompletableFuture<List<MultiAsset>>> multiAssetFutures = new ArrayList<>();
    long multiAssetSize = multiAssetRepository.count();

    // Calculate the total number of pages needed to process all the multi-assets in batches.
    long totalPage = multiAssetSize % subListSize == 0 ?
                     multiAssetSize / subListSize :
                     multiAssetSize / subListSize + 1;
    var startTime = System.currentTimeMillis();

    // Fetch multi-assets data in parallel by dividing them into batches (pages).
    for (int page = 0; page < totalPage; page++) {
      final int _page = page;
      multiAssetFutures.add(CompletableFuture.supplyAsync(
          () -> {
            log.info("Page {}", _page);
            return jooqMultiAssetRepository.getMultiAsset(
                _page, subListSize);
          }).exceptionally(e -> {
        throw new RuntimeException("Exception occurs when getting multiAsset list", e);
      }));
    }

    // Collect the results from all CompletableFuture instances to get the list of multi-assets.
    var multiAssetList = multiAssetFutures.stream().map(CompletableFuture::join)
        .flatMap(List::stream)
        .collect(Collectors.toList());

    log.info("Get MultiAsset take {} ms", System.currentTimeMillis() - startTime);

    Timestamp time24hAgo =
        Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC).minusDays(1));

    // Find the minimum transaction ID that occurred after the time 24 hours ago.
    Long txId = txRepository.findMinTxByAfterTime(time24hAgo).orElse(Long.MAX_VALUE);

    // Define the maximum batch size for processing multi-assets.
    int multiAssetListSize = 50000;

    // Process the multi-assets in batches to build token info data.
    for (int i = 0; i < multiAssetList.size(); i += multiAssetListSize) {
      int toIndex = Math.min(i + multiAssetListSize, multiAssetList.size());
      var subList = multiAssetList.subList(i, toIndex);

      tokenInfoFutures.add(
          tokenInfoServiceAsync.buildTokenInfoList(subList, maxBlockNo, txId, updateTime)
              .exceptionally(e -> {
                throw new RuntimeException("Exception occurs when initializing token info list",
                    e);
              }));

      // After every 5 batches, insert the fetched token info data into the database in batches.
      if (tokenInfoFutures.size() % 5 == 0) {
        var tokenInfoList = tokenInfoFutures.stream().map(CompletableFuture::join)
            .flatMap(List::stream)
            .toList();
        BatchUtils.doBatching(1000, tokenInfoList, jooqTokenInfoRepository::insertAll);
        tokenInfoFutures.clear();
      }
    }

    // Wait for the remaining CompletableFuture instances to complete.
    CompletableFuture.allOf(tokenInfoFutures.toArray(new CompletableFuture[0]))
        .join();
    List<TokenInfo> tokenInfoList = tokenInfoFutures.stream()
        .map(CompletableFuture::join)
        .flatMap(List::stream)
        .toList();
    BatchUtils.doBatching(1000, tokenInfoList, jooqTokenInfoRepository::insertAll);

    multiAssetList.clear();

    tokenInfoCheckpointRepository.save(
        TokenInfoCheckpoint.builder().blockNo(maxBlockNo)
            .updateTime(updateTime).build());
  }

  /**
   * Update existing token info by inserting new data and updating existing ones.
   *
   * @param tokenInfoCheckpoint The latest token info checkpoint.
   * @param maxBlockNo          The maximum block number.
   * @param updateTime          The update time.
   */
  private void updateExistingTokenInfo(TokenInfoCheckpoint tokenInfoCheckpoint, Long maxBlockNo,
                                       Timestamp updateTime) {
    Timestamp time24hAgo =
        Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
    var lastUpdateTime = tokenInfoCheckpoint.getUpdateTime();

    if (lastUpdateTime.compareTo(time24hAgo) < 0) {
      return;
    }

    Long txId = txRepository.findMinTxByAfterTime(time24hAgo).orElse(Long.MAX_VALUE);

    // Retrieve multi-assets involved in transactions between the last processed block and the latest block.
    var tokensInTransactionWithNewBlockRange = multiAssetRepository.getTokensInTransactionInBlockRange(
        tokenInfoCheckpoint.getBlockNo(),
        maxBlockNo);
    log.info("tokensInTransactionWithNewBlockRange has size: {}",
        tokensInTransactionWithNewBlockRange.size());
    var tokensInTransactionWithNewBlockRangeMap = StreamUtil.toMap(
        tokensInTransactionWithNewBlockRange, MultiAsset::getId, Function.identity());

    // Retrieve multi-assets with zero transaction counts that were updated after the last update time.
    var tokensWithZeroTxCount = multiAssetRepository.getTokensWithZeroTxCountAndAfterTime(
        tokenInfoCheckpoint.getUpdateTime());
    log.info("tokensWithZeroTxCount has size: {}", tokensWithZeroTxCount.size());
    var tokensWithZeroTxCountMap = StreamUtil.toMap(
        tokensWithZeroTxCount, MultiAsset::getId, Function.identity());

    // Retrieve multi-assets involved in transactions
    // from 24h before last update time to 24h before the current time
    var tokenNeedUpdateVolume24h = multiAssetRepository.getTokensInTransactionInTimeRange(
        Timestamp.valueOf(lastUpdateTime.toLocalDateTime().minusDays(1)), time24hAgo
    );

    log.info("tokenNeedUpdateVolume24h has size: {}", tokenNeedUpdateVolume24h.size());
    var tokenNeedUpdateVolume24hMap = StreamUtil.toMap(
        tokenNeedUpdateVolume24h, MultiAsset::getId, Function.identity());

    // Create a map that merges all the multi-assets that need to be processed in this update.
    var tokenToProcessMap = new HashMap<Long, MultiAsset>();
    tokenToProcessMap.putAll(tokensInTransactionWithNewBlockRangeMap);
    tokenToProcessMap.putAll(tokensWithZeroTxCountMap);
    tokenToProcessMap.putAll(tokenNeedUpdateVolume24hMap);

    log.info("tokenToProcess has size: {}", tokenToProcessMap.size());

    // Process the tokens to update the corresponding TokenInfo entities in batches of 10,000.
    BatchUtils.doBatching(10000, tokenToProcessMap.values().stream().toList(), multiAssets -> {
      // Create a list of multi-asset IDs to process in this batch.
      List<Long> multiAssetIds = StreamUtil.mapApply(multiAssets, MultiAsset::getId);
      List<TokenInfo> saveEntities = new ArrayList<>();
      List<TokenVolume> volumes = jooqAddressTokenRepository.sumBalanceAfterTx(
          multiAssetIds, txId);
      var tokenVolumeMap =
          StreamUtil.toMap(
              volumes, TokenVolume::getIdent, TokenVolume::getVolume);
      var mapNumberHolder = multiAssetDataProcessorService.getMapNumberHolder(multiAssetIds);

      var tokenInfoMap = tokenInfoRepository.findByMultiAssetIdIn(
              multiAssetIds).stream()
          .collect(Collectors.toMap(TokenInfo::getMultiAssetId, Function.identity()));

      multiAssetIds.forEach(multiAssetId -> {
        var tokenInfo = tokenInfoMap.getOrDefault(multiAssetId, new TokenInfo());
        tokenInfo.setMultiAsset(tokenToProcessMap.get(multiAssetId));
        tokenInfo.setVolume24h(tokenVolumeMap.getOrDefault(multiAssetId, BigInteger.ZERO));
        tokenInfo.setNumberOfHolders(mapNumberHolder.getOrDefault(multiAssetId, 0L));
        tokenInfo.setUpdateTime(updateTime);
        tokenInfo.setBlockNo(maxBlockNo);
        saveEntities.add(tokenInfo);
      });

      BatchUtils.doBatching(500, saveEntities, tokenInfoRepository::saveAll);
    });

    tokenInfoCheckpointRepository.save(
        TokenInfoCheckpoint.builder().blockNo(maxBlockNo)
            .updateTime(updateTime).build());
  }


}