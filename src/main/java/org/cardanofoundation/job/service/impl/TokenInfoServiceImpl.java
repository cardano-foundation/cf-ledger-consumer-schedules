package org.cardanofoundation.job.service.impl;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.consumercommon.entity.MultiAsset;
import org.cardanofoundation.explorer.consumercommon.entity.TokenInfo;
import org.cardanofoundation.explorer.consumercommon.entity.TokenInfoCheckpoint;
import org.cardanofoundation.job.projection.TokenNumberHoldersProjection;
import org.cardanofoundation.job.projection.TokenVolumeProjection;
import org.cardanofoundation.job.repository.AddressTokenBalanceRepository;
import org.cardanofoundation.job.repository.AddressTokenRepository;
import org.cardanofoundation.job.repository.BlockRepository;
import org.cardanofoundation.job.repository.MultiAssetRepository;
import org.cardanofoundation.job.repository.TokenInfoCheckpointRepository;
import org.cardanofoundation.job.repository.TokenInfoRepository;
import org.cardanofoundation.job.repository.TxRepository;
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
  private final AddressTokenBalanceRepository addressTokenBalanceRepository;

  @Override
  @Transactional
  @SneakyThrows
  public void updateTokenInfoList() {
    Optional<Long> maxBlockNo = blockRepository.findMaxBlocKNo();
    if (maxBlockNo.isEmpty()) {
      return;
    }

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
          Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).minusDays(1));
      Long txId = txRepository.findMinTxByAfterTime(yesterday).orElse(Long.MAX_VALUE);

      int multiAssetListSize = 50000;

      for (int i = 0; i < multiAssetList.size(); i += multiAssetListSize) {
        int toIndex = Math.min(i + multiAssetListSize, multiAssetList.size());
        var subList = multiAssetList.subList(i, toIndex);

        tokenInfoFutures.add(
            tokenInfoServiceAsync.initTokenInfoList(subList, maxBlockNo.get(), txId)
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
    } else {
      Timestamp yesterday =
          Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).minusDays(1));
      Long txId = txRepository.findMinTxByAfterTime(yesterday).orElse(Long.MAX_VALUE);
      var tokensInTransaction = multiAssetRepository.getTokensInTransaction(
          tokenInfoCheckpoint.get().getBlockNo(),
          maxBlockNo.get());

      var tokensWithZeroTxCount = multiAssetRepository.getTokensWithZeroTxCountAndAfterTime(
          tokenInfoCheckpoint.get().getUpdateTime());

      List<MultiAsset> tokensToProcess = new ArrayList<>();
      tokensToProcess.addAll(tokensInTransaction);
      tokensToProcess.addAll(tokensWithZeroTxCount);
      BatchUtils.doBatching(10000, tokensToProcess, tokens -> {
        List<TokenInfo> saveEntities = new ArrayList<>();
        var multiAssetIds = StreamUtil.mapApply(tokens, MultiAsset::getId);
        List<TokenVolumeProjection> volumes = addressTokenRepository.sumBalanceAfterTx(
            tokens, txId);
        var tokenVolumeMap =
            StreamUtil.toMap(
                volumes, TokenVolumeProjection::getIdent, TokenVolumeProjection::getVolume);
        var mapNumberHolder = tokenInfoServiceAsync.getMapNumberHolder(multiAssetIds);
        var tokenInfoMap = tokenInfoRepository.findByFingerprintIn(
                StreamUtil.mapApplySet(tokens, MultiAsset::getFingerprint)).stream()
            .collect(Collectors.toMap(TokenInfo::getFingerprint, Function.identity()));

        var updateTime = Timestamp.from(Instant.now());

        tokens.forEach(multiAsset -> {
          var tokenInfo = tokenInfoMap.getOrDefault(multiAsset.getFingerprint(), new TokenInfo());
          tokenInfo.setFingerprint(multiAsset.getFingerprint());
          tokenInfo.setVolume24h(tokenVolumeMap.getOrDefault(multiAsset.getId(), BigInteger.ZERO));
          tokenInfo.setNumberOfHolders(mapNumberHolder.getOrDefault(multiAsset.getId(), 0L));
          tokenInfo.setUpdateTime(updateTime);
          tokenInfo.setBlockNo(maxBlockNo.get());
          saveEntities.add(tokenInfo);
        });
        BatchUtils.doBatching(500, saveEntities, tokenInfoRepository::saveAll);
      });
    }

    tokenInfoCheckpointRepository.save(
        TokenInfoCheckpoint.builder().blockNo(maxBlockNo.get())
            .updateTime(Timestamp.from(Instant.now())).build());
  }
}
