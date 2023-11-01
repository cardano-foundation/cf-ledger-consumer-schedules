package org.cardanofoundation.job.repository.explorer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.consumercommon.explorer.entity.StakeKeyReportHistory;

public interface StakeKeyReportHistoryRepository
    extends JpaRepository<StakeKeyReportHistory, Long> {

  StakeKeyReportHistory findByReportHistoryId(@Param("reportHistoryId") Long reportHistoryId);
}
