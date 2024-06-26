package org.cardanofoundation.job.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Service;

import org.cardanofoundation.job.model.TokenNumberHolders;
import org.cardanofoundation.job.repository.ledgersync.LatestTokenBalanceRepository;
import org.cardanofoundation.job.service.MultiAssetService;
import org.cardanofoundation.job.util.StreamUtil;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MultiAssetServiceImpl implements MultiAssetService {

  LatestTokenBalanceRepository latestTokenBalanceRepository;

  /**
   * Build a mapping of multiAsset IDs to the total number of holders for each multi-asset. The
   * method calculates the number of holders for each multi-asset by combining two counts: 1. The
   * number of holders with stake address associated with the multi-asset 2. The number of holders
   * without stake address associated with the multi-asset
   *
   * @param multiAssetIds The list of multi-asset IDs for which to calculate the number of holders.
   * @return A map containing multi-asset IDs as keys and the total number of holders as values.
   */
  @Override
  public Map<Long, Long> getMapNumberHolder(List<Long> multiAssetIds) {
    var numberOfHolders = latestTokenBalanceRepository.countHoldersByMultiAssetIdIn(multiAssetIds);

    var numberHoldersMap =
        StreamUtil.toMap(
            numberOfHolders, TokenNumberHolders::getIdent, TokenNumberHolders::getNumberOfHolders);

    return multiAssetIds.stream()
        .collect(
            Collectors.toMap(ident -> ident, ident -> numberHoldersMap.getOrDefault(ident, 0L)));
  }

  /**
   * Build a mapping of multiAsset IDs to the total number of holders for each multi-asset. The
   * method calculates the number of holders for each multi-asset by combining two counts: 1. The
   * number of holders with stake address associated with the multi-asset 2. The number of holders
   * without stake address associated with the multi-asset
   *
   * @param startIdent The starting multi-asset ID.
   * @param endIdent The ending multi-asset ID.
   * @return A map containing multi-asset IDs as keys and the total number of holders as values.
   */
  @Override
  public Map<Long, Long> getMapNumberHolder(Long startIdent, Long endIdent) {
    var numberOfHolders =
        latestTokenBalanceRepository.countHoldersByMultiAssetIdInRange(startIdent, endIdent);

    var numberHoldersMap =
        StreamUtil.toMap(
            numberOfHolders, TokenNumberHolders::getIdent, TokenNumberHolders::getNumberOfHolders);

    return LongStream.rangeClosed(startIdent, endIdent)
        .boxed()
        .collect(
            Collectors.toMap(ident -> ident, ident -> numberHoldersMap.getOrDefault(ident, 0L)));
  }
}
