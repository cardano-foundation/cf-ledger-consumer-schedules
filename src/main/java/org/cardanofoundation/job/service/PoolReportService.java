package org.cardanofoundation.job.service;

import org.cardanofoundation.explorer.consumercommon.entity.PoolReportHistory;

public interface PoolReportService {

  /**
   * Generate Excel file for pool report, then push it to storage
   *
   * @param poolReportHistory pool report history
   */
  void exportPoolReport(PoolReportHistory poolReportHistory) throws Exception;

  /**
   * Find pool report history by report id
   *
   * @param reportId report id
   * @return pool report history
   */
  PoolReportHistory findByReportId(Long reportId);
}
