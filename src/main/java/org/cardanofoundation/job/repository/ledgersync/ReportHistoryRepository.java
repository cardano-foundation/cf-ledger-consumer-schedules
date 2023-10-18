package org.cardanofoundation.job.repository.ledgersync;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.consumercommon.entity.ReportHistory;

public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long> {

  @Query("SELECT r FROM ReportHistory r WHERE r.uploadedAt < :uploadedAt AND r.status <> 'EXPIRED'")
  List<ReportHistory> findNotExpiredReportHistoryByUploadedAtLessThan(
      @Param("uploadedAt") Timestamp uploadedAt);
}
