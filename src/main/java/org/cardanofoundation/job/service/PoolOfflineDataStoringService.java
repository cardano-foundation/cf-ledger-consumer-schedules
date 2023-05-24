package org.cardanofoundation.job.service;

import java.util.Set;

import org.cardanofoundation.job.dto.PoolData;

public interface PoolOfflineDataStoringService {

  void insertBatch(Set<PoolData> successPools);
}
