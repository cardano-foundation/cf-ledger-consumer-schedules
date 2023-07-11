package org.cardanofoundation.job.service.impl;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.cardanofoundation.explorer.consumercommon.entity.PoolReportHistory;
import org.cardanofoundation.explorer.consumercommon.entity.StakeKeyReportHistory;
import org.cardanofoundation.job.config.datasource.DataBaseType;
import org.cardanofoundation.job.config.datasource.SwitchDataSource;
import org.cardanofoundation.job.repository.PoolReportHistoryRepository;
import org.cardanofoundation.job.repository.StakeKeyReportHistoryRepository;
import org.cardanofoundation.job.service.ReportHistoryService;

@Service
@RequiredArgsConstructor
public class ReportHistoryServiceImpl implements ReportHistoryService {

  private final StakeKeyReportHistoryRepository stakeKeyReportHistoryRepository;
  private final PoolReportHistoryRepository poolReportHistoryRepository;

  @Override
  @SwitchDataSource(DataBaseType.ANALYTICS)
  public void saveStakeReportHistory(StakeKeyReportHistory stakeKeyReportHistory) {
    stakeKeyReportHistoryRepository.save(stakeKeyReportHistory);
  }

  @Override
  @SwitchDataSource(DataBaseType.ANALYTICS)
  public void savePoolReportHistory(PoolReportHistory poolReportHistory) {
    poolReportHistoryRepository.save(poolReportHistory);
  }
}
