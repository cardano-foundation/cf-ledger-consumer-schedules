package com.sotatek.cardano.job.service.interfaces;

import com.sotatek.cardano.job.dto.PoolData;
import java.util.Set;

public interface PoolOfflineDataStoringService {

  void insertBatch(Set<PoolData> successPools);
}
