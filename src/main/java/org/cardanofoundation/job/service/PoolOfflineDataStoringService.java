package org.cardanofoundation.job.service;

import java.util.List;

import org.cardanofoundation.job.dto.PoolData;

public interface PoolOfflineDataStoringService {

  /**
   * Saving the fetched success offline data
   *
   * @param successPools the fetched success pools
   */
  void saveSuccessPoolOfflineData(List<PoolData> successPools);

  /**
   * Saving the fetched fail offline data
   *
   * @param failedPools the fetched fail pools
   */
  void saveFailOfflineData(List<PoolData> failedPools);
}
