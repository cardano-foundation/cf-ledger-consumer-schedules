package org.cardanofoundation.job.service;

import org.cardanofoundation.explorer.consumercommon.analytics.entity.PoolReportHistory;

public interface PoolReportService {

  void exportPoolReport(PoolReportHistory poolReportHistory) throws Exception;
}
