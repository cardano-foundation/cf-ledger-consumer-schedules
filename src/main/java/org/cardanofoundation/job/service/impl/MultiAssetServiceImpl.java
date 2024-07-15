package org.cardanofoundation.job.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import org.cardanofoundation.job.repository.ledgersync.AddressTxAmountRepository;
import org.cardanofoundation.job.service.MultiAssetService;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MultiAssetServiceImpl implements MultiAssetService {

  //  LatestTokenBalanceRepository latestTokenBalanceRepository;
  private final AddressTxAmountRepository addressTxAmountRepository;

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
    Map<String, Long> map = new HashMap<>();
    try {
      var numberOfHolders = addressTxAmountRepository.countHoldersByMultiAssetIdInRange(units);
      if (numberOfHolders != null) {
        log.info("numberOfHolders is not null, size: {}", numberOfHolders.size());
        numberOfHolders.forEach(holder -> map.put(holder.getUnit(), holder.getNumberOfHolders()));
      }
      return map;
    } catch (Exception e) {
      log.error("Error Token", e);
      return map;
    }
  }
}
