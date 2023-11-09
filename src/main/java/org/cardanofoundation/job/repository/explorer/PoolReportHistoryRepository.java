package org.cardanofoundation.job.repository.explorer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.consumercommon.explorer.entity.PoolReportHistory;

public interface PoolReportHistoryRepository extends JpaRepository<PoolReportHistory, Long> {

  PoolReportHistory findByReportHistoryId(@Param("reportHistoryId") Long reportHistoryId);
}
