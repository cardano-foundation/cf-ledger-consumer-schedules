package org.cardanofoundation.job.service;

import java.util.List;

import org.cardanofoundation.job.dto.PoolData;

public interface PoolOfflineDataStoringService {

  /**
   *
   *  Insert success pool
   * @param successPools
   */
  void insertSuccessPoolOfflineData(List<PoolData> successPools);

  /**
   * Insert fail pool
   * @param failedPools
   */
  void insertFailOfflineData(List<PoolData> failedPools);
}
