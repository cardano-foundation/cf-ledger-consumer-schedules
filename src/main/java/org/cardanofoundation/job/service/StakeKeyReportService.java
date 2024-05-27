package org.cardanofoundation.job.service;

public interface StakeKeyReportService {

  /**
   * Generate Excel file for stake key report, then push it to storage
   *
   * @param stakeKeyReportHistory stake key report history
   */
  void exportStakeKeyReport(Long reportId) throws Exception;
}
