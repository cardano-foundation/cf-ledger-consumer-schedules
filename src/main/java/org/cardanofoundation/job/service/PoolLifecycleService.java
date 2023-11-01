package org.cardanofoundation.job.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import org.cardanofoundation.explorer.consumercommon.explorer.entity.PoolReportHistory;
import org.cardanofoundation.job.dto.report.pool.DeRegistrationResponse;
import org.cardanofoundation.job.dto.report.pool.EpochSize;
import org.cardanofoundation.job.dto.report.pool.PoolUpdateDetailResponse;
import org.cardanofoundation.job.dto.report.pool.RewardResponse;
import org.cardanofoundation.job.dto.report.pool.TabularRegisResponse;

public interface PoolLifecycleService {

  List<TabularRegisResponse> registrationList(String poolView, Pageable pageable);

  List<PoolUpdateDetailResponse> poolUpdateList(String poolView, Pageable pageable);

  List<RewardResponse> listReward(PoolReportHistory poolReportHistory, Pageable pageable);

  List<DeRegistrationResponse> deRegistration(String poolView, Pageable pageable);

  List<EpochSize> getPoolSizes(PoolReportHistory poolReportHistory, Pageable pageable);
}
