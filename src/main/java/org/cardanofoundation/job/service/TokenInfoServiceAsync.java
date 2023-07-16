package org.cardanofoundation.job.service;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.consumercommon.entity.MultiAsset;
import org.cardanofoundation.explorer.consumercommon.entity.TokenInfo;
import org.cardanofoundation.job.model.TokenNumberHolders;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.repository.jooq.JOOQAddressTokenBalanceRepository;
import org.cardanofoundation.job.repository.jooq.JOOQAddressTokenRepository;
import org.cardanofoundation.job.util.StreamUtil;

@Component
@RequiredArgsConstructor
@Log4j2
public class TokenInfoServiceAsync {

  private final JOOQAddressTokenRepository jooqAddressTokenRepository;
  private final JOOQAddressTokenBalanceRepository jooqAddressTokenBalanceRepository;

  @Async
  @Transactional(readOnly = true)
  public CompletableFuture<List<TokenInfo>> initTokenInfoList(
      List<MultiAsset> multiAssetList, Long blockNo, Long afterTxId) {

    List<TokenInfo> saveEntities = new ArrayList<>(multiAssetList.size());
    var curTime = System.currentTimeMillis();
    List<Long> multiAssetIds = StreamUtil.mapApply(multiAssetList, MultiAsset::getId);
    List<TokenVolume> volumes =
        jooqAddressTokenRepository.sumBalanceAfterTx(multiAssetIds, afterTxId);

    var tokenVolumeMap =
        StreamUtil.toMap(
            volumes, TokenVolume::getIdent, TokenVolume::getVolume);
    var mapNumberHolder = getMapNumberHolder(multiAssetIds);

    var updateTime = Timestamp.from(Instant.now());

    multiAssetIds.clear();
    volumes.clear();

    multiAssetList.forEach(multiAsset -> {
      var tokenInfo = new TokenInfo();
      tokenInfo.setFingerprint(multiAsset.getFingerprint());
      tokenInfo.setVolume24h(tokenVolumeMap.getOrDefault(multiAsset.getId(), BigInteger.ZERO));
      tokenInfo.setNumberOfHolders(mapNumberHolder.getOrDefault(multiAsset.getId(), 0L));
      tokenInfo.setUpdateTime(updateTime);
      tokenInfo.setBlockNo(blockNo);
      saveEntities.add(tokenInfo);
    });

    log.info("getTokenInfoListNeedSave take {} ms", System.currentTimeMillis() - curTime);

    return CompletableFuture.completedFuture(saveEntities);
  }

  public Map<Long, Long> getMapNumberHolder(List<Long> multiAssetIds) {
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
