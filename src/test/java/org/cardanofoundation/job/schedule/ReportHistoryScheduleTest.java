package org.cardanofoundation.job.schedule;

import java.sql.Timestamp;
import java.util.Collections;
import org.cardanofoundation.explorer.consumercommon.entity.ReportHistory;
import org.cardanofoundation.explorer.consumercommon.enumeration.ReportStatus;
import org.cardanofoundation.explorer.consumercommon.enumeration.ReportType;
import org.cardanofoundation.job.repository.ledgersync.ReportHistoryRepository;
import org.cardanofoundation.job.schedules.ReportHistorySchedule;
import org.cardanofoundation.job.service.impl.StorageReportServiceImpl;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReportHistoryScheduleTest {

  @Mock
  StorageReportServiceImpl storageService;

  @Mock
  ReportHistoryRepository reportHistoryRepository;
  ReportHistorySchedule reportHistorySchedule;

  @BeforeEach
  void setUp() {
    reportHistorySchedule = new ReportHistorySchedule(reportHistoryRepository, storageService);
  }

  @Test
  void setExpiredReportHistory_logicTest() {
    ReportHistory reportHistory = spy(ReportHistory.builder()
        .username("username")
        .storageKey("storageKey")
        .type(ReportType.STAKE_KEY)
        .status(ReportStatus.GENERATED)
        .createdAt(new Timestamp(System.currentTimeMillis()))
        .id(1L)
        .build());

    when(reportHistoryRepository.findNotExpiredReportHistoryByUploadedAtLessThan(any(Timestamp.class)))
        .thenReturn(Collections.singletonList(reportHistory));

    reportHistorySchedule.setExpiredReportHistory();
    verify(storageService, times(1)).deleteFile(reportHistory.getStorageKey());
    assertEquals(ReportStatus.EXPIRED, reportHistory.getStatus());
  }
}