package org.cardanofoundation.job.service;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.common.entity.explorer.TokenInfo;
import org.cardanofoundation.job.model.TokenTxCount;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.repository.ledgersync.AddressTxAmountRepository;
import org.cardanofoundation.job.util.StreamUtil;

@Component
@RequiredArgsConstructor
@Log4j2
public class TokenInfoServiceAsync {

  private final AddressTxAmountRepository addressTxAmountRepository;
  private final MultiAssetService multiAssetService;

  /**
   * Asynchronously builds a list of TokenInfo entities based on the provided list of MultiAsset.
   * This method is called when initializing TokenInfo data
   *
   * @param startIdent The starting multi-asset ID.
   * @param endIdent The ending multi-asset ID.
   * @param blockNo The maximum block number to set for the TokenInfo entities.
   * @param afterTxId The transaction ID from which to calculate token volumes.
   * @param updateTime The timestamp to set as the update time for the TokenInfo entities.
   * @return A CompletableFuture containing the list of TokenInfo entities built from the provided
   *     MultiAsset list.
   */
  @Async
  @Transactional(readOnly = true)
  public CompletableFuture<List<TokenInfo>> buildTokenInfoList(
      Long startIdent, Long endIdent, Long blockNo, Long afterTxId, Timestamp updateTime) {

    List<TokenInfo> saveEntities = new ArrayList<>((int) (endIdent - startIdent + 1));
    var curTime = System.currentTimeMillis();
    List<Long> multiAssetIds =
        LongStream.rangeClosed(startIdent, endIdent).boxed().collect(Collectors.toList());
    List<TokenVolume> volumes24h =
        addressTxAmountRepository.sumBalanceAfterTx(startIdent, endIdent, afterTxId);

    List<TokenVolume> totalVolumes =
        addressTxAmountRepository.getTotalVolumeByIdentInRange(startIdent, endIdent);

    List<TokenTxCount> txCounts =
        addressTxAmountRepository.getTotalTxCountByIdentInRange(startIdent, endIdent);

    var tokenVolume24hMap =
        StreamUtil.toMap(volumes24h, TokenVolume::getIdent, TokenVolume::getVolume);
    var totalVolumeMap =
        StreamUtil.toMap(totalVolumes, TokenVolume::getIdent, TokenVolume::getVolume);
    var txCountMap = StreamUtil.toMap(txCounts, TokenTxCount::getIdent, TokenTxCount::getTxCount);
    var mapNumberHolder = multiAssetService.getMapNumberHolder(startIdent, endIdent);

    // Clear unnecessary lists to free up memory to avoid OOM error
    volumes24h.clear();
    totalVolumes.clear();
    txCounts.clear();

    multiAssetIds.forEach(
        multiAssetId -> {
          var tokenInfo = new TokenInfo();
          tokenInfo.setMultiAssetId(multiAssetId);
          tokenInfo.setVolume24h(tokenVolume24hMap.getOrDefault(multiAssetId, BigInteger.ZERO));
          tokenInfo.setTotalVolume(totalVolumeMap.getOrDefault(multiAssetId, BigInteger.ZERO));
          tokenInfo.setTxCount(txCountMap.getOrDefault(multiAssetId, 0L));
          tokenInfo.setNumberOfHolders(mapNumberHolder.getOrDefault(multiAssetId, 0L));
          tokenInfo.setUpdateTime(updateTime);
          tokenInfo.setBlockNo(blockNo);
          saveEntities.add(tokenInfo);
        });

    log.info("getTokenInfoListNeedSave take {} ms", System.currentTimeMillis() - curTime);

    return CompletableFuture.completedFuture(saveEntities);
  }
}
