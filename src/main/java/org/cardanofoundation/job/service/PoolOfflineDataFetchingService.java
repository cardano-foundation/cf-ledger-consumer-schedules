package org.cardanofoundation.job.service;

import java.util.stream.Stream;

import org.cardanofoundation.job.dto.PoolData;

public interface PoolOfflineDataFetchingService {

  /**
   * Insert PoolOfflineData and PoolOfflineFetchError with batch with Map object key is hash String
   * and value is PoolOfflineData | PoolOfflineFetchError If map size equal to batch size,
   * PoolOfflineData would be committed.
   *
   * @param start start position
   */
  int fetchPoolOfflineDataByBatch(Integer start);

  /**
   * Fetch extended logo and icon field from success pool data
   * @param stream
   */
  void fetchPoolOfflineDataLogo(Stream<PoolData> stream);
}
