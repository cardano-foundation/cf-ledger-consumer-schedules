package org.cardanofoundation.job.service;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.consumercommon.entity.MultiAsset;
import org.cardanofoundation.explorer.consumercommon.explorer.entity.TokenInfo;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.repository.ledgersync.jooq.JOOQAddressTokenRepository;
import org.cardanofoundation.job.util.StreamUtil;

@Component
@RequiredArgsConstructor
@Log4j2
public class TokenInfoServiceAsync {

  private final JOOQAddressTokenRepository jooqAddressTokenRepository;
  private final MultiAssetService multiAssetService;

  /**
   * Asynchronously builds a list of TokenInfo entities based on the provided list of MultiAsset.
   * This method is called when initializing TokenInfo data
   *
   * @param multiAssetList The list of MultiAsset to process.
   * @param blockNo        The maximum block number to set for the TokenInfo entities.
   * @param afterTxId      The transaction ID from which to calculate token volumes.
   * @param updateTime     The timestamp to set as the update time for the TokenInfo entities.
   * @return A CompletableFuture containing the list of TokenInfo entities built from the provided
   * MultiAsset list.
   */
  @Async
  @Transactional(readOnly = true)
  public CompletableFuture<List<TokenInfo>> buildTokenInfoList(
      List<MultiAsset> multiAssetList, Long blockNo, Long afterTxId, Timestamp updateTime) {

    List<TokenInfo> saveEntities = new ArrayList<>(multiAssetList.size());
    var curTime = System.currentTimeMillis();
    List<Long> multiAssetIds = StreamUtil.mapApply(multiAssetList, MultiAsset::getId);
    List<TokenVolume> volumes =
        jooqAddressTokenRepository.sumBalanceAfterTx(multiAssetIds, afterTxId);

    var tokenVolumeMap =
        StreamUtil.toMap(
            volumes, TokenVolume::getIdent, TokenVolume::getVolume);
    var mapNumberHolder = multiAssetService.getMapNumberHolder(multiAssetIds);

    // Clear unnecessary lists to free up memory to avoid OOM error
    multiAssetIds.clear();
    volumes.clear();

    multiAssetList.forEach(multiAsset -> {
      var tokenInfo = new TokenInfo();
      tokenInfo.setMultiAssetId(multiAsset.getId());
      tokenInfo.setVolume24h(tokenVolumeMap.getOrDefault(multiAsset.getId(), BigInteger.ZERO));
      tokenInfo.setNumberOfHolders(mapNumberHolder.getOrDefault(multiAsset.getId(), 0L));
      tokenInfo.setUpdateTime(updateTime);
      tokenInfo.setBlockNo(blockNo);
      saveEntities.add(tokenInfo);
    });

    log.info("getTokenInfoListNeedSave take {} ms", System.currentTimeMillis() - curTime);

    return CompletableFuture.completedFuture(saveEntities);
  }

}
