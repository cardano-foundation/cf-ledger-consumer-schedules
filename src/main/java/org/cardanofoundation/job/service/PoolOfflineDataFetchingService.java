package org.cardanofoundation.job.service;

import java.util.List;

import org.cardanofoundation.job.dto.PoolData;

public interface PoolOfflineDataFetchingService {

  /**
   * Fetch pool offline data.
   *
   * @return the poll data list
   */
  List<PoolData> fetchPoolOfflineData();

  /**
   * Fetch pool offline data logo. then update to poolDataSuccess list
   *
   * @param poolDataSuccess the pool offline data fetched success
   */
  void fetchPoolOfflineDataLogo(List<PoolData> poolDataSuccess);
}
