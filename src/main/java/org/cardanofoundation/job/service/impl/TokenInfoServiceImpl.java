package org.cardanofoundation.job.service.impl;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.cardanofoundation.job.model.TokenNumberHolders;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.projection.TokenVolumeProjection;
import org.cardanofoundation.job.repository.AddressTokenRepository;
import org.cardanofoundation.job.repository.BlockRepository;
import org.cardanofoundation.job.repository.MultiAssetRepository;
import org.cardanofoundation.job.repository.TokenInfoCheckpointRepository;
import org.cardanofoundation.job.repository.TokenInfoRepository;
import org.cardanofoundation.job.repository.TxRepository;
import org.cardanofoundation.job.repository.jooq.JOOQAddressTokenBalanceRepository;
import org.cardanofoundation.job.repository.jooq.JOOQAddressTokenRepository;
import org.cardanofoundation.job.repository.jooq.JOOQMultiAssetRepository;
import org.cardanofoundation.job.repository.jooq.JOOQTokenInfoRepository;
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
  private final AddressTokenRepository addressTokenRepository;
  private final JOOQTokenInfoRepository jooqTokenInfoRepository;
  private final JOOQMultiAssetRepository jooqMultiAssetRepository;
  private final JOOQAddressTokenBalanceRepository jooqAddressTokenBalanceRepository;
  private final JOOQAddressTokenRepository jooqAddressTokenRepository;

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
    var updateTime = timeLatestBlock.toLocalDateTime();

    var tokenInfoCheckpoint = tokenInfoCheckpointRepository.findLatestTokenInfoCheckpoint();

    if (tokenInfoCheckpoint.isEmpty()) {
      log.info("Init token info data for the first time");
      int subListSize = 10000;

      List<CompletableFuture<List<TokenInfo>>> tokenInfoFutures = new ArrayList<>();
      List<CompletableFuture<List<MultiAsset>>> multiAssetFutures = new ArrayList<>();
      long multiAssetSize = multiAssetRepository.count();
      long totalPage = multiAssetSize % subListSize == 0 ?
                       multiAssetSize / subListSize :
                       multiAssetSize / subListSize + 1;
      var startTime = System.currentTimeMillis();
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
      var multiAssetList = multiAssetFutures.stream().map(CompletableFuture::join)
          .flatMap(List::stream)
          .collect(Collectors.toList());

      log.info("Get MultiAsset take {} ms", System.currentTimeMillis() - startTime);

      Timestamp yesterday =
          Timestamp.valueOf(updateTime.minusDays(1));
      Long txId = txRepository.findMinTxByAfterTime(yesterday).orElse(Long.MAX_VALUE);

      int multiAssetListSize = 50000;

      for (int i = 0; i < multiAssetList.size(); i += multiAssetListSize) {
        int toIndex = Math.min(i + multiAssetListSize, multiAssetList.size());
        var subList = multiAssetList.subList(i, toIndex);

        tokenInfoFutures.add(
            tokenInfoServiceAsync.initTokenInfoList(subList, maxBlockNo, txId,
                    Timestamp.valueOf(updateTime))
                .exceptionally(e -> {
                  throw new RuntimeException("Exception occurs when initializing token info list",
                      e);
                }));
        if (tokenInfoFutures.size() % 5 == 0) {
          var tokenInfoList = tokenInfoFutures.stream().map(CompletableFuture::join)
              .flatMap(List::stream)
              .toList();
          BatchUtils.doBatching(1000, tokenInfoList, jooqTokenInfoRepository::insertAll);
          tokenInfoFutures.clear();
        }
      }
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
              .updateTime(Timestamp.valueOf(updateTime)).build());

    } else {
      Timestamp yesterday =
          Timestamp.valueOf(updateTime.minusDays(1));
      Long txId = txRepository.findMinTxByAfterTime(yesterday).orElse(Long.MAX_VALUE);
      var tokensInTransactionWithNewBlockRange = multiAssetRepository.getTokensInTransactionInBlockRange(
          tokenInfoCheckpoint.get().getBlockNo(),
          maxBlockNo);
      log.info("tokensInTransactionWithNewBlockRange has size: {}", tokensInTransactionWithNewBlockRange.size());
      var lastUpdateTime = tokenInfoCheckpoint.get().getUpdateTime();

      var tokensWithZeroTxCount = multiAssetRepository.getTokensWithZeroTxCountAndAfterTime(
          tokenInfoCheckpoint.get().getUpdateTime());
      log.info("tokensWithZeroTxCount has size: {}", tokensWithZeroTxCount.size());

      var tokenNeedUpdateVolume24h = multiAssetRepository.getTokensInTransactionInTimeRange(
          Timestamp.valueOf(lastUpdateTime.toLocalDateTime().minusDays(1)), yesterday
      );
      log.info("tokenNeedUpdateVolume24h has size: {}", tokenNeedUpdateVolume24h.size());

      Set<Long> idsOfTokensToProcess = new HashSet<>();
      idsOfTokensToProcess.addAll(
          StreamUtil.mapApply(tokensInTransactionWithNewBlockRange, MultiAsset::getId));
      idsOfTokensToProcess.addAll(StreamUtil.mapApply(tokensWithZeroTxCount, MultiAsset::getId));
      idsOfTokensToProcess.addAll(StreamUtil.mapApply(tokenNeedUpdateVolume24h, MultiAsset::getId));

      log.info("tokensToProcess has size: {}", idsOfTokensToProcess.size());

      BatchUtils.doBatching(10000, idsOfTokensToProcess.stream().toList(), multiAssetIds -> {
        List<TokenInfo> saveEntities = new ArrayList<>();

        List<TokenVolume> volumes = jooqAddressTokenRepository.sumBalanceAfterTx(
            multiAssetIds, txId);
        var tokenVolumeMap =
            StreamUtil.toMap(
                volumes, TokenVolume::getIdent, TokenVolume::getVolume);
        var mapNumberHolder = getMapNumberHolder(multiAssetIds);
        var tokenInfoMap = tokenInfoRepository.findByMultiAssetIdIn(
                multiAssetIds).stream()
            .collect(Collectors.toMap(TokenInfo::getMultiAssetId, Function.identity()));

        multiAssetIds.forEach(id -> {
          var tokenInfo = tokenInfoMap.getOrDefault(id, new TokenInfo());
          tokenInfo.setMultiAssetId(id);
          tokenInfo.setVolume24h(tokenVolumeMap.getOrDefault(id, BigInteger.ZERO));
          tokenInfo.setNumberOfHolders(mapNumberHolder.getOrDefault(id, 0L));
          tokenInfo.setUpdateTime(Timestamp.valueOf(updateTime));
          tokenInfo.setBlockNo(maxBlockNo);
          saveEntities.add(tokenInfo);
        });
        BatchUtils.doBatching(500, saveEntities, tokenInfoRepository::saveAll);

      });

      tokenInfoCheckpointRepository.save(
          TokenInfoCheckpoint.builder().blockNo(maxBlockNo)
              .updateTime(Timestamp.valueOf(updateTime)).build());
    }

  }

  private Map<Long, Long> getMapNumberHolder(List<Long> multiAssetIds) {
    var numberOfHoldersWithStakeKey =
        jooqAddressTokenBalanceRepository.countByMultiAssetIn(multiAssetIds);
    var numberOfHoldersWithAddressNotHaveStakeKey =
        jooqAddressTokenBalanceRepository.countAddressNotHaveStakeByMultiAssetIn(multiAssetIds);

    var numberHoldersStakeKeyMap =
        StreamUtil.toMap(
            numberOfHoldersWithStakeKey,
            TokenNumberHolders::getIdent,
            TokenNumberHolders::getNumberOfHolders);
    var numberHoldersAddressNotHaveStakeKeyMap =
        StreamUtil.toMap(
            numberOfHoldersWithAddressNotHaveStakeKey,
            TokenNumberHolders::getIdent,
            TokenNumberHolders::getNumberOfHolders);
    return multiAssetIds.stream()
        .collect(
            Collectors.toMap(
                ident -> ident,
                ident ->
                    numberHoldersStakeKeyMap.getOrDefault(ident, 0L)
                        + numberHoldersAddressNotHaveStakeKeyMap.getOrDefault(ident, 0L)));
  }
}
