package com.sotatek.cardano.job.service.interfaces;

import java.util.Set;

import com.sotatek.cardano.job.dto.PoolData;

public interface PoolOfflineDataStoringService {

  void insertBatch(Set<PoolData> successPools);
}
