package org.cardanofoundation.job.service;

import org.cardanofoundation.explorer.common.entity.explorer.StakeKeyReportHistory;

public interface StakeKeyReportService {

  /**
   * Generate Excel file for stake key report, then push it to storage
   *
   * @param stakeKeyReportHistory stake key report history
   */
  void exportStakeKeyReport(StakeKeyReportHistory stakeKeyReportHistory, Long zoneOffset, String timePattern) throws Exception;
}
