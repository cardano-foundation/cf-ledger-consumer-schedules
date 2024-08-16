package org.cardanofoundation.job.service;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.explorer.common.entity.explorer.TokenInfo;
import org.cardanofoundation.explorer.common.entity.explorer.TokenInfoCheckpoint;
import org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.repository.explorer.TokenInfoCheckpointRepository;
import org.cardanofoundation.job.repository.explorer.TokenInfoRepository;
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
  private final TokenInfoCheckpointRepository tokenInfoCheckpointRepository;
  private final TokenInfoRepository tokenInfoRepository;

  // We need requires new so that just this block of code is run in isolation
  @Transactional(value = "explorerTransactionManager", propagation = Propagation.REQUIRES_NEW)
  public TokenInfoCheckpoint processTokenInRange(Long fromSlot, Long toSlot, Long currentSlot) {

    TokenInfoCheckpoint checkpoint =
        TokenInfoCheckpoint.builder()
            .slot(toSlot)
            .updateTime(Timestamp.valueOf(LocalDateTime.now()))
            .build();
    long start = System.currentTimeMillis();
    Set<String> unitMultiAssetCollection =
        addressTxAmountRepository.getTokensInTransactionInSlotRange(fromSlot, toSlot);
    log.info(
        "Processing token info from slot: {} to slot: {}, with number of units in transaction is {}",
        fromSlot,
        toSlot,
        unitMultiAssetCollection.size());

    if (CollectionUtils.isEmpty(unitMultiAssetCollection)) {
      return checkpoint;
    }

    CompletableFuture<List<TokenInfo>> tokenInfoListFuture =
        buildTokenInfoList(unitMultiAssetCollection, fromSlot, toSlot, currentSlot);

    List<TokenInfo> tokenInfoList = tokenInfoListFuture.join();

    saveTokenInfoListToDbInRollbackCaseMightNotOccur(
        tokenInfoList, unitMultiAssetCollection, toSlot);

    tokenInfoCheckpointRepository.save(checkpoint); // new checkpoint

    log.info(
        "Token info processing from slot: {} to slot: {} took: {} ms",
        fromSlot,
        toSlot,
        System.currentTimeMillis() - start);
    return checkpoint;
  }

  // A safety slot is a time slot that is guaranteed to be in the past, usually equal to the tip
  // minus 24 hours.
  public void processTokenFromSafetySlot(Long latestProcessedSlot, Long tip) {
    Set<String> unitMultiAssetCollection =
        addressTxAmountRepository.getTokensInTransactionInSlotRange(latestProcessedSlot, tip);

    if (CollectionUtils.isEmpty(unitMultiAssetCollection)) {
      return;
    }

    // to leverage the async capabilities of the service
    // public CompletableFuture<List<TokenInfo>> buildTokenInfoList(
    //      Set<String> units, Long fromSlot, Long toSlot, Long currentSlot)
    // the toSlot must be the tip because we need calculate from the latest processed slot to the
    // tip

    CompletableFuture<List<TokenInfo>> tokenInfoListFuture =
        selfProxyService.buildTokenInfoList(
            unitMultiAssetCollection, latestProcessedSlot, tip, tip);

    List<TokenInfo> tokenInfoList = tokenInfoListFuture.join();
    saveTokenInfoListToDbInRollbackCaseMightOccur(
        tokenInfoList, unitMultiAssetCollection, latestProcessedSlot);
  }

  private void saveTokenInfoListToDbInRollbackCaseMightNotOccur(
      List<TokenInfo> tokenInfoList, Set<String> unitMultiAssetCollection, Long toSlot) {

    Map<String, TokenInfo> tokenInfoMap =
        tokenInfoRepository.findByUnitIn(unitMultiAssetCollection).stream()
            .collect(Collectors.toMap(TokenInfo::getUnit, Function.identity()));

    tokenInfoList.forEach(
        tokenInfo -> {
          if (tokenInfoMap.containsKey(tokenInfo.getUnit())) {
            // if the token is already in the database
            // then update the previous values with the existing values and the updated values with
            // the new values
            TokenInfo existingTokenInfo = tokenInfoMap.get(tokenInfo.getUnit());
            if (existingTokenInfo.getUpdatedSlot() > toSlot
                && Objects.isNull(existingTokenInfo.getPreviousSlot())) {
              // case: The updated slot is greater than the latest processed slot and there is no
              // previous slot
              // meaning: The token is updated in a slot that might be rolled back
              // then: update the updated values and the previous value
              // note that: current slot ( toSlot) is the trusted slot that will not be rolled back
              tokenInfo.setPreviousNumberOfHolders(tokenInfo.getNumberOfHolders());
              tokenInfo.setPreviousTxCount(tokenInfo.getTxCount());
              tokenInfo.setPreviousTotalVolume(tokenInfo.getTotalVolume());
              tokenInfo.setPreviousVolume24h(tokenInfo.getVolume24h());
              tokenInfo.setPreviousSlot(tokenInfo.getUpdatedSlot());
            } else if (existingTokenInfo.getUpdatedSlot() > toSlot
                && Objects.nonNull(existingTokenInfo.getPreviousSlot())) {
              // case: The updated slot is greater than the latest processed slot and there is a
              // previous slot
              // meaning: The token is updated in a slot that might be rolled back
              // then: modify the updated values and not modify the previous values
              tokenInfo.setTotalVolume(
                  existingTokenInfo.getPreviousTotalVolume().add(tokenInfo.getTotalVolume()));
            } else if (existingTokenInfo.getUpdatedSlot() < toSlot) {
              // case: the updated slot is less than the latest processed slot
              // meaning: The updated slot and the previous slot are trusted.
              // then: update the updated values with new values (equal to the sum of the previous
              // values and the values calculated in the current slot)
              // the previous values must be the updated values of the existing token
              tokenInfo.setPreviousNumberOfHolders(existingTokenInfo.getNumberOfHolders());
              tokenInfo.setPreviousTxCount(existingTokenInfo.getTxCount());
              tokenInfo.setPreviousTotalVolume(existingTokenInfo.getTotalVolume());
              tokenInfo.setPreviousVolume24h(existingTokenInfo.getVolume24h());
              tokenInfo.setPreviousSlot(existingTokenInfo.getUpdatedSlot());
              tokenInfo.setTotalVolume(
                  existingTokenInfo.getTotalVolume().add(tokenInfo.getTotalVolume()));
            }
          } else {
            // if the token is not in the database
            // then the previous values must be equal to the updated values
            tokenInfo.setPreviousNumberOfHolders(tokenInfo.getNumberOfHolders());
            tokenInfo.setPreviousTxCount(tokenInfo.getTxCount());
            tokenInfo.setPreviousTotalVolume(tokenInfo.getTotalVolume());
            tokenInfo.setPreviousVolume24h(tokenInfo.getVolume24h());
            tokenInfo.setPreviousSlot(tokenInfo.getUpdatedSlot());
          }
        });
    tokenInfoRepository.saveAllAndFlush(tokenInfoList);
  }

  // The function is used in cases where data is saved to the database and a rollback might occur.
  private void saveTokenInfoListToDbInRollbackCaseMightOccur(
      List<TokenInfo> tokenInfoList,
      Set<String> unitMultiAssetCollection,
      Long latestProcessedSlot) {
    Map<String, TokenInfo> tokenInfoMap =
        tokenInfoRepository.findByUnitIn(unitMultiAssetCollection).stream()
            .collect(Collectors.toMap(TokenInfo::getUnit, Function.identity()));
    tokenInfoList.forEach(
        tokenInfo -> {
          // if the token is already in the database
          if (tokenInfoMap.containsKey(tokenInfo.getUnit())) {
            TokenInfo existingTokenInfo = tokenInfoMap.get(tokenInfo.getUnit());
            if (existingTokenInfo.getUpdatedSlot() > latestProcessedSlot
                && Objects.isNull(existingTokenInfo.getPreviousSlot())) {
              // case: The updated slot is greater than the latest processed slot and there is no
              // previous slot
              // meaning: The token is updated in a slot that might be rolled back
              // then: only update the updated values, the previous values must be null
              tokenInfo.setTotalVolume(tokenInfo.getTotalVolume());
            } else if (existingTokenInfo.getUpdatedSlot() > latestProcessedSlot
                && Objects.nonNull(existingTokenInfo.getPreviousSlot())) {
              // case: The updated slot is greater than the latest processed slot and there is a
              // previous slot
              // meaning: The token is updated in a slot that might be rolled back
              // then: The updated values must be added to the previous values
              tokenInfo.setTotalVolume(
                  existingTokenInfo.getPreviousTotalVolume().add(tokenInfo.getTotalVolume()));
            } else if (existingTokenInfo.getUpdatedSlot() <= latestProcessedSlot) {
              // case: the updated slot is less than or equal to the latest processed slot
              // meaning: The updated slot is trusted.
              // then: The previous values must be updated with the existing values
              // and the updated values with the new values
              tokenInfo.setPreviousNumberOfHolders(existingTokenInfo.getNumberOfHolders());
              tokenInfo.setPreviousTxCount(existingTokenInfo.getTxCount());
              tokenInfo.setPreviousTotalVolume(existingTokenInfo.getTotalVolume());
              tokenInfo.setPreviousVolume24h(existingTokenInfo.getVolume24h());
              tokenInfo.setPreviousSlot(existingTokenInfo.getUpdatedSlot());
              tokenInfo.setTotalVolume(
                  existingTokenInfo.getTotalVolume().add(tokenInfo.getTotalVolume()));
            }
          }
        });

    tokenInfoRepository.saveAllAndFlush(tokenInfoList);
  }

  @Async
  @Transactional(readOnly = true)
  public CompletableFuture<List<TokenInfo>> buildTokenInfoList(
      Set<String> units, Long fromSlot, Long toSlot, Long currentSlot) {
    var curTime = System.currentTimeMillis();

    List<TokenInfo> saveEntities = buildTokenInfo(units, fromSlot, toSlot, currentSlot);

    log.info(
        "Build token list with size: {} took: {}ms",
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
      Set<String> multiAssetUnits, Long toSlot, Long currentSlot) {
    List<TokenVolume> tokenVolumes = new ArrayList<>();

    LocalDateTime toTime = cardanoConverters.slot().slotToTime(toSlot);
    LocalDateTime realTime = cardanoConverters.slot().slotToTime(currentSlot);

    Long slot24hAgo = cardanoConverters.time().toSlot(realTime.minusHours(24));

    // meaning if toSlot is less than 24h ago from the current slot (current time - tip).
    // then we must be calculating the volume24h values
    if (ChronoUnit.HOURS.between(toTime, realTime) <= 24) {
      tokenVolumes =
          addressTxAmountRepository.sumBalanceAfterBlockTime(multiAssetUnits, slot24hAgo);
    }
    return CompletableFuture.completedFuture(tokenVolumes);
  }

  @Async
  public CompletableFuture<List<TokenVolume>> getTotalVolume(
      Set<String> multiAssetUnits, Long fromSlot, Long toSlot) {
    List<TokenVolume> tokenVolumes =
        addressTxAmountRepository.getTotalVolumeByUnits(multiAssetUnits, fromSlot, toSlot);
    return CompletableFuture.completedFuture(tokenVolumes);
  }

  @Async
  public CompletableFuture<Map<String, Long>> getMapNumberHolderByUnits(
      Set<String> multiAssetUnits) {
    Map<String, Long> mapNumberHolder =
        multiAssetService.getMapNumberHolderByUnits(multiAssetUnits);
    return CompletableFuture.completedFuture(mapNumberHolder);
  }

  private List<TokenInfo> buildTokenInfo(
      Set<String> multiAssetUnits, Long fromSlot, Long toSlot, Long realSlot) {

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
