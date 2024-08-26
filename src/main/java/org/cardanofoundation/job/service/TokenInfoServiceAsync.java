package org.cardanofoundation.job.service;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.explorer.common.entity.explorer.TokenInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.util.StreamUtil;

@Component
@RequiredArgsConstructor
@Log4j2
public class TokenInfoServiceAsync {

  @Autowired @Lazy TokenInfoServiceAsync selfProxyService;

  private final AddressTxAmountRepository addressTxAmountRepository;
  private final MultiAssetService multiAssetService;
  private final CardanoConverters cardanoConverters;

  @Transactional(readOnly = true)
  public List<TokenInfo> buildTokenInfoList(
      List<String> units, Long fromSlot, Long toSlot, Long currentSlot) {
    var curTime = System.currentTimeMillis();

    List<TokenInfo> saveEntities = buildTokenInfo(units, fromSlot, toSlot, currentSlot);

    log.info(
        "Build token list with size: {} took: {}ms",
        saveEntities.size(),
        System.currentTimeMillis() - curTime);
    return saveEntities;
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
      List<String> multiAssetUnits, Long toSlot, Long currentSlot) {
    long startTime = System.currentTimeMillis();
    List<TokenVolume> tokenVolumes = new ArrayList<>();

    LocalDateTime toTime = cardanoConverters.slot().slotToTime(toSlot);
    LocalDateTime realTime = cardanoConverters.slot().slotToTime(currentSlot);

    Long slot24hAgo = cardanoConverters.time().toSlot(realTime.minusHours(24));

    // meaning if toSlot is less than 24h ago from the current slot (current time - tip).
    // then we must be calculating the volume24h values
    if (ChronoUnit.HOURS.between(toTime, realTime) <= 24) {
      tokenVolumes = addressTxAmountRepository.sumBalanceAfterSlot(multiAssetUnits, slot24hAgo);
    }
    log.info("Processing getVolume24h took: {}ms", System.currentTimeMillis() - startTime);
    return CompletableFuture.completedFuture(tokenVolumes);
  }

  @Async
  public CompletableFuture<List<TokenVolume>> getTotalVolume(
      List<String> multiAssetUnits, Long fromSlot, Long toSlot) {
    long startTime = System.currentTimeMillis();
    List<TokenVolume> tokenVolumes =
        addressTxAmountRepository.getTotalVolumeByUnits(multiAssetUnits, fromSlot, toSlot);
    log.info(
        "Processing getTotalVolume took: {}ms, from slot {} to slot {}",
        System.currentTimeMillis() - startTime,
        fromSlot,
        toSlot);
    return CompletableFuture.completedFuture(tokenVolumes);
  }

  @Async
  public CompletableFuture<Map<String, Long>> getMapNumberHolderByUnits(
      List<String> multiAssetUnits) {
    long startTime = System.currentTimeMillis();
    Map<String, Long> mapNumberHolder =
        multiAssetService.getMapNumberHolderByUnits(multiAssetUnits);
    log.info(
        "Processing getMapNumberHolderByUnits took: {}ms", System.currentTimeMillis() - startTime);
    return CompletableFuture.completedFuture(mapNumberHolder);
  }

  private List<TokenInfo> buildTokenInfo(
      List<String> multiAssetUnits, Long fromSlot, Long toSlot, Long realSlot) {

    List<TokenInfo> saveEntities = new ArrayList<>();

    // Get the volume 24h, total volume, and number of holders for each multi-asset unit
    // Should be call by selfProxyService to make it async
    CompletableFuture<List<TokenVolume>> volumes24hFuture =
        selfProxyService.getVolume24h(multiAssetUnits, toSlot, realSlot);

    CompletableFuture<List<TokenVolume>> totalVolumesFuture =
        selfProxyService.getTotalVolume(multiAssetUnits, fromSlot, toSlot);

    CompletableFuture<Map<String, Long>> mapNumberHolderFuture =
        selfProxyService.getMapNumberHolderByUnits(multiAssetUnits);

    // Wait for all the async calls to complete
    CompletableFuture.allOf(volumes24hFuture, totalVolumesFuture, mapNumberHolderFuture).join();

    // Get the results of the async calls
    List<TokenVolume> volumes24h = volumes24hFuture.join();
    List<TokenVolume> totalVolumes = totalVolumesFuture.join();
    Map<String, Long> mapNumberHolder = mapNumberHolderFuture.join();

    Map<String, BigInteger> tokenVolume24hMap =
        StreamUtil.toMap(volumes24h, TokenVolume::getUnit, TokenVolume::getVolume);
    Map<String, BigInteger> totalVolumeMap =
        StreamUtil.toMap(totalVolumes, TokenVolume::getUnit, TokenVolume::getVolume);

    multiAssetUnits.forEach(
        unit -> {
          TokenInfo tokenInfo = new TokenInfo();
          tokenInfo.setUnit(unit);
          tokenInfo.setVolume24h(tokenVolume24hMap.getOrDefault(unit, BigInteger.ZERO));
          tokenInfo.setTotalVolume(totalVolumeMap.getOrDefault(unit, BigInteger.ZERO));
          tokenInfo.setNumberOfHolders(mapNumberHolder.getOrDefault(unit, 0L));
          tokenInfo.setUpdatedSlot(toSlot);
          saveEntities.add(tokenInfo);
        });

    return saveEntities;
  }
}
