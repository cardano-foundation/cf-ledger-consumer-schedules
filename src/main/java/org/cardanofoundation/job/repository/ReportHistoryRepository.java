package org.cardanofoundation.job.repository;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.consumercommon.entity.ReportHistory;

public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long> {

  List<ReportHistory> findByUploadedAtLessThan(Timestamp uploadedAt);
}
