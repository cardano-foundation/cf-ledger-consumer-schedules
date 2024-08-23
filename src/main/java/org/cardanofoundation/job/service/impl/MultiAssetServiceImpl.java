package org.cardanofoundation.job.service.impl;

import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Service;

import org.cardanofoundation.job.model.TokenNumberHolders;
import org.cardanofoundation.job.repository.ledgersyncagg.LatestTokenBalanceRepository;
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
   * @param units units of multi-asset
   * @return A map containing multi-asset IDs as keys and the total number of holders as values.
   */
  @Override
  public Map<String, Long> getMapNumberHolderByUnits(List<String> units) {
    var numberOfHolders = latestTokenBalanceRepository.countHoldersByMultiAssetIdInRange(units);
    return StreamUtil.toMap(
        numberOfHolders, TokenNumberHolders::getUnit, TokenNumberHolders::getNumberOfHolders);
  }
}
