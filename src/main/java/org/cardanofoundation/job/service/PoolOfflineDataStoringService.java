package org.cardanofoundation.job.service;

import java.util.List;
import java.util.Queue;

import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.job.dto.PoolData;

public interface PoolOfflineDataStoringService {

  /**
   * Insert success pool
   *
   * @param successPools
   */
  @Transactional
  void insertSuccessPoolOfflineData(Queue<PoolData> successPools);

  /**
   * Insert fail pool
   *
   * @param failedPools
   */
  @Transactional
  void insertFailOfflineData(List<PoolData> failedPools);
}
