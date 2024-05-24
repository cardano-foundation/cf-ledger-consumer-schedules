package org.cardanofoundation.job.repository.explorer;

import java.sql.Timestamp;
import java.util.List;

import org.cardanofoundation.explorer.common.entity.enumeration.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.explorer.ReportHistory;

public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long> {

  @Query("SELECT r FROM ReportHistory r WHERE r.uploadedAt < :uploadedAt AND r.status <> 'EXPIRED'")
  List<ReportHistory> findNotExpiredReportHistoryByUploadedAtLessThan(
      @Param("uploadedAt") Timestamp uploadedAt);

  @Query("SELECT r FROM ReportHistory r WHERE r.status = :status")
  List<ReportHistory> findAllReportHistoryByStatus(@Param("status") ReportStatus status);
}
