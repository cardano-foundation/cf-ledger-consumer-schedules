package org.cardanofoundation.job.service;

import org.cardanofoundation.explorer.consumercommon.entity.PoolReportHistory;
import org.cardanofoundation.explorer.consumercommon.entity.StakeKeyReportHistory;

public interface ReportHistoryService {

  /**
   * Save stake key report history
   * @param stakeKeyReportHistory
   */
  void saveStakeReportHistory(StakeKeyReportHistory stakeKeyReportHistory);

  /**
   * Save pool report history
   * @param poolReportHistory
   */
  void savePoolReportHistory(PoolReportHistory poolReportHistory);
}
