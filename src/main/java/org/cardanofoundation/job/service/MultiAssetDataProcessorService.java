package org.cardanofoundation.job.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.cardanofoundation.job.model.TokenNumberHolders;
import org.cardanofoundation.job.repository.jooq.JOOQAddressTokenBalanceRepository;
import org.cardanofoundation.job.util.StreamUtil;

@Service
@RequiredArgsConstructor
public class MultiAssetDataProcessorService {

  private final JOOQAddressTokenBalanceRepository jooqAddressTokenBalanceRepository;

  /**
   * Build a mapping of multiAsset IDs to the total number of holders for each multi-asset. The
   * method calculates the number of holders for each multi-asset by combining two counts: 1. The
   * number of holders with stake address associated with the multi-asset 2. The number of holders
   * without stake address associated with the multi-asset
   *
   * @param multiAssetIds The list of multi-asset IDs for which to calculate the number of holders.
   * @return A map containing multi-asset IDs as keys and the total number of holders as values.
   */
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
