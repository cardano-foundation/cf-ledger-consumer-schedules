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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.apache.hadoop.shaded.org.eclipse.jetty.websocket.common.frames.DataFrame;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;
import org.cardanofoundation.explorer.common.entity.explorer.TokenInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.projection.TokenUnitProjection;
import org.cardanofoundation.job.repository.ledgersync.MultiAssetRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.util.StreamUtil;

@Component
@RequiredArgsConstructor
@Log4j2
public class TokenInfoServiceAsync {

  @Autowired @Lazy TokenInfoServiceAsync selfProxyService;

  private final AddressTxAmountRepository addressTxAmountRepository;
  private final MultiAssetRepository multiAssetRepository;
  private final MultiAssetService multiAssetService;
  private final SparkSession sparkSession;
  private final SQLContext sqlContext;


  @Value("${multi-datasource.datasourceLedgerSyncAgg.hikariConfig.jdbcUrl.hikariConfig.jdbcUrl}")
  private String jdbcUrl;

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
      Long startIdent,
      Long endIdent,
      Long blockNo,
      Long epochSecond24hAgo,
      Timestamp timeLatestBlock) {

    var curTime = System.currentTimeMillis();

    Map<String, Long> multiAssetUnitMap =
        multiAssetRepository.getTokenUnitByIdBetween(startIdent, endIdent).stream()
            .collect(Collectors.toMap(TokenUnitProjection::getUnit, TokenUnitProjection::getIdent));

    List<String> multiAssetUnits = new ArrayList<>(multiAssetUnitMap.keySet());

    List<TokenInfo> saveEntities =
        buildTokenInfo(
            blockNo, epochSecond24hAgo, timeLatestBlock, multiAssetUnits, multiAssetUnitMap);

    log.info(
        "getTokenInfoListNeedSave startIdent: {} endIdent: {} took: {}ms",
        startIdent,
        endIdent,
        System.currentTimeMillis() - curTime);

    return CompletableFuture.completedFuture(saveEntities);
  }

  @Async
  @Transactional(readOnly = true)
  public CompletableFuture<List<TokenInfo>> buildTokenInfoList(
      List<String> units, Long blockNo, Long epochSecond24hAgo, Timestamp timeLatestBlock) {
    var curTime = System.currentTimeMillis();

    Map<String, Long> multiAssetUnitMap =
        multiAssetRepository.getTokenUnitByUnitIn(units).stream()
            .collect(Collectors.toMap(TokenUnitProjection::getUnit, TokenUnitProjection::getIdent));

    List<TokenInfo> saveEntities =
        buildTokenInfo(blockNo, epochSecond24hAgo, timeLatestBlock, units, multiAssetUnitMap);

    log.info(
        "getTokenInfoListNeedSave size: {} took: {}ms",
        saveEntities.size(),
        System.currentTimeMillis() - curTime);
    return CompletableFuture.completedFuture(saveEntities);
  }

  @Async
  @Transactional(readOnly = true)
  public CompletableFuture<List<TokenTxCount>> buildTokenTxCountList(List<String> units) {
    long startTime = System.currentTimeMillis();
    List<TokenTxCount> tokenTxCounts = addressTxAmountRepository.getTotalTxCountByUnitIn(units);
    log.info(
        "buildTokenTxCountList size: {}, took: {}ms",
        tokenTxCounts.size(),
        System.currentTimeMillis() - startTime);
    return CompletableFuture.completedFuture(tokenTxCounts);
  }

  @Async
  public CompletableFuture<List<TokenVolume>> getVolume24h(
      List<String> multiAssetUnits, Long epochSecond24hAgo, Long epochSecondLastTimeBlock) {
    List<TokenVolume> tokenVolumes = new ArrayList<>();

    if (epochSecond24hAgo <= epochSecondLastTimeBlock) {
      tokenVolumes =
          addressTxAmountRepository.sumBalanceAfterBlockTime(multiAssetUnits, epochSecond24hAgo);
    }
    return CompletableFuture.completedFuture(tokenVolumes);
  }

  @Async
  public CompletableFuture<List<TokenVolume>> getTotalVolume(List<String> multiAssetUnits) {
    List<TokenVolume> tokenVolumes =
        addressTxAmountRepository.getTotalVolumeByUnits(multiAssetUnits);
    return CompletableFuture.completedFuture(tokenVolumes);
  }

  @Async
  public CompletableFuture<Map<String, Long>> getMapNumberHolderByUnits(
      List<String> multiAssetUnits) {
    Map<String, Long> mapNumberHolder =
        multiAssetService.getMapNumberHolderByUnits(multiAssetUnits);
    return CompletableFuture.completedFuture(mapNumberHolder);
  }

  private List<TokenInfo> buildTokenInfo(
      Long blockNo,
      Long epochSecond24hAgo,
      Timestamp timeLatestBlock,
      List<String> multiAssetUnits,
      Map<String, Long> multiAssetUnitMap) {

    List<TokenInfo> saveEntities = new ArrayList<>();

    // Get the volume 24h, total volume, and number of holders for each multi-asset unit
    // Should be call by selfProxyService to make it async
    CompletableFuture<List<TokenVolume>> volumes24hFuture =
        selfProxyService.getVolume24h(
            multiAssetUnits, epochSecond24hAgo, timeLatestBlock.toInstant().getEpochSecond());

    CompletableFuture<List<TokenVolume>> totalVolumesFuture =
        selfProxyService.getTotalVolume(multiAssetUnits);

    CompletableFuture<Map<String, Long>> mapNumberHolderFuture =
        selfProxyService.getMapNumberHolderByUnits(multiAssetUnits);

    // Wait for all the async calls to complete
    CompletableFuture.allOf(volumes24hFuture, totalVolumesFuture, mapNumberHolderFuture).join();

    // Get the results of the async calls
    List<TokenVolume> volumes24h = volumes24hFuture.join();
    List<TokenVolume> totalVolumes = totalVolumesFuture.join();
    Map<String, Long> mapNumberHolder = mapNumberHolderFuture.join();

    var tokenVolume24hMap =
        StreamUtil.toMap(volumes24h, TokenVolume::getUnit, TokenVolume::getVolume);
    var totalVolumeMap =
        StreamUtil.toMap(totalVolumes, TokenVolume::getUnit, TokenVolume::getVolume);

    multiAssetUnits.forEach(
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

    return saveEntities;
  }
}
