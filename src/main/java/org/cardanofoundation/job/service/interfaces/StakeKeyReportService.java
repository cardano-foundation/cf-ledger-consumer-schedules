package org.cardanofoundation.job.service.interfaces;

import org.cardanofoundation.explorer.consumercommon.entity.StakeKeyReportHistory;

public interface StakeKeyReportService {

  /**
   * Generate Excel file for stake key report, then push it to storage
   *
   * @param stakeKeyReportHistory stake key report history
   */
  void exportStakeKeyReport(StakeKeyReportHistory stakeKeyReportHistory);
}
