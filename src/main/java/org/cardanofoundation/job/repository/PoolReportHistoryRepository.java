package org.cardanofoundation.job.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.consumercommon.entity.PoolReportHistory;

public interface PoolReportHistoryRepository extends JpaRepository<PoolReportHistory, Long> {

  PoolReportHistory findByReportHistoryId(@Param("reportHistoryId") Long reportHistoryId);
}
