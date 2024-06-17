package org.cardanofoundation.job.service;

import java.math.BigInteger;
import java.sql.Timestamp;
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

import org.cardanofoundation.explorer.common.entity.explorer.TokenInfo;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.projection.TokenUnitProjection;
import org.cardanofoundation.job.repository.ledgersync.AddressTxAmountRepository;
import org.cardanofoundation.job.repository.ledgersync.MultiAssetRepository;
import org.cardanofoundation.job.util.StreamUtil;

@Component
@RequiredArgsConstructor
@Log4j2
public class TokenInfoServiceAsync {

  private final AddressTxAmountRepository addressTxAmountRepository;
  private final MultiAssetService multiAssetService;
  private final MultiAssetRepository multiAssetRepository;

  /**
   * Asynchronously builds a list of TokenInfo entities based on the provided list of MultiAsset.
   * This method is called when initializing TokenInfo data
   *
   * @param startIdent The starting multi-asset ID.
   * @param endIdent The ending multi-asset ID.
   * @param blockNo The maximum block number to set for the TokenInfo entities.
   * @param epochSecond24hAgo epochSecond 24 hours ago
   * @param timeLatestBlock The timestamp to set as the update time for the TokenInfo entities.
   * @return A CompletableFuture containing the list of TokenInfo entities built from the provided
   *     MultiAsset list.
   */
  @Async
  @Transactional(readOnly = true)
  public CompletableFuture<List<TokenInfo>> buildTokenInfoList(
      Long startIdent, Long endIdent, Long blockNo, Long epochSecond24hAgo, Timestamp timeLatestBlock) {

    List<TokenInfo> saveEntities = new ArrayList<>((int) (endIdent - startIdent + 1));
    var startTime = System.currentTimeMillis();
    var curTime = startTime;
    Map<String, Long> multiAssetUnitMap =
        multiAssetRepository.getTokenUnitByIdBetween(startIdent, endIdent).stream()
            .collect(Collectors.toMap(TokenUnitProjection::getUnit, TokenUnitProjection::getIdent));

    log.info("getMultiAssetUnitMap startIdent: {} endIdent: {} took: {}ms", startIdent, endIdent, System.currentTimeMillis() - curTime);
    curTime = System.currentTimeMillis();

    List<String> multiAssetUnits = new ArrayList<>(multiAssetUnitMap.keySet());
    List<TokenVolume> volumes24h = new ArrayList<>();
    // if epochSecond24hAgo > epochTime of timeLatestBlock then ignore calculation of 24h volume
    if (epochSecond24hAgo <= timeLatestBlock.toInstant().getEpochSecond()) {
      volumes24h = addressTxAmountRepository.sumBalanceAfterBlockTime(multiAssetUnits, epochSecond24hAgo);
    }
    log.info("get24hVolume startIdent: {} endIdent: {} took: {}ms", startIdent, endIdent, System.currentTimeMillis() - curTime);
    curTime = System.currentTimeMillis();

    List<TokenVolume> totalVolumes =
        addressTxAmountRepository.getTotalVolumeByUnits(multiAssetUnits);

    log.info("getTotalVolume startIdent: {} endIdent: {} took: {}ms", startIdent, endIdent, System.currentTimeMillis() - curTime);
    curTime = System.currentTimeMillis();

    var tokenVolume24hMap =
        StreamUtil.toMap(volumes24h, TokenVolume::getUnit, TokenVolume::getVolume);
    var totalVolumeMap =
        StreamUtil.toMap(totalVolumes, TokenVolume::getUnit, TokenVolume::getVolume);
    var mapNumberHolder = multiAssetService.getMapNumberHolderByUnits(multiAssetUnits);
    log.info("getMapNumberHolderByUnits startIdent: {} endIdent: {} took: {}ms", startIdent, endIdent, System.currentTimeMillis() - curTime);

    // Clear unnecessary lists to free up memory to avoid OOM error
    volumes24h.clear();
    totalVolumes.clear();

    multiAssetUnits.parallelStream().forEach(
        unit -> {
          var tokenInfo = new TokenInfo();
          tokenInfo.setMultiAssetId(multiAssetUnitMap.get(unit));
          tokenInfo.setVolume24h(tokenVolume24hMap.getOrDefault(unit, BigInteger.ZERO));
          tokenInfo.setTotalVolume(totalVolumeMap.getOrDefault(unit, BigInteger.ZERO));
          tokenInfo.setNumberOfHolders(mapNumberHolder.getOrDefault(unit, 0L));
          tokenInfo.setUpdateTime(timeLatestBlock);
          tokenInfo.setBlockNo(blockNo);
          saveEntities.add(tokenInfo);
        });

    log.info(
        "getTokenInfoListNeedSave startIdent: {} endIdent: {} took: {}ms",
        startIdent,
        endIdent,
        System.currentTimeMillis() - startTime);

    return CompletableFuture.completedFuture(saveEntities);
  }
}
