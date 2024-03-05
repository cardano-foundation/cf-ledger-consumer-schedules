package org.cardanofoundation.job.service;

import java.util.List;

import org.cardanofoundation.job.common.enumeration.PoolActionType;
import org.cardanofoundation.job.dto.PoolCertificateHistory;

public interface PoolCertificateService {

  List<PoolCertificateHistory> getPoolCertificateByAction(
      String poolViewOrHash, PoolActionType action);
}
