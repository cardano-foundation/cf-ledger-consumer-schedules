package org.cardanofoundation.job.service;

import org.cardanofoundation.explorer.common.entity.explorer.PoolReportHistory;

public interface PoolReportService {

  void exportPoolReport(PoolReportHistory poolReportHistory) throws Exception;
}
