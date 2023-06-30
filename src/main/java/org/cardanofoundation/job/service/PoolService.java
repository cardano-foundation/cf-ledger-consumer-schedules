package org.cardanofoundation.job.service;

import org.cardanofoundation.job.dto.PoolStatus;

public interface PoolService {

  PoolStatus getCurrentPoolStatus();
}
