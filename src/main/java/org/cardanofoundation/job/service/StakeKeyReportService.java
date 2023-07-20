package org.cardanofoundation.job.service;

import org.cardanofoundation.explorer.consumercommon.entity.StakeKeyReportHistory;

public interface StakeKeyReportService {

  /**
   * Generate Excel file for stake key report, then push it to storage
   *
   * @param stakeKeyReportHistory stake key report history
   */
  void exportStakeKeyReport(StakeKeyReportHistory stakeKeyReportHistory) throws Exception;

  /**
   * Find stake key report history by report id
   *
   * @param reportId report id
   * @return stake key report history
   */
  StakeKeyReportHistory findByReportId(Long reportId);
}
