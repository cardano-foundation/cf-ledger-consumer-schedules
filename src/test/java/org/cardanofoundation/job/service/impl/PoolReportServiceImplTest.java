package org.cardanofoundation.job.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.explorer.common.entity.enumeration.ReportStatus;
import org.cardanofoundation.explorer.common.entity.enumeration.ReportType;
import org.cardanofoundation.explorer.common.entity.explorer.PoolReportHistory;
import org.cardanofoundation.explorer.common.entity.explorer.ReportHistory;
import org.cardanofoundation.job.repository.explorer.PoolReportHistoryRepository;
import org.cardanofoundation.job.service.ReportHistoryServiceAsync;
import org.cardanofoundation.job.util.report.ExcelHelper;
import org.cardanofoundation.job.util.report.ExportContent;

@ExtendWith(MockitoExtension.class)
class PoolReportServiceImplTest {

  @Mock ReportHistoryServiceAsync reportHistoryServiceAsync;

  @Mock ExcelHelper excelHelper;

  @Mock PoolReportHistoryRepository poolReportRepository;

  @Mock StorageReportServiceImpl storageService;

  @InjectMocks PoolReportServiceImpl poolReportService;

  @Test
  void exportPoolReport_shouldThrowExceptionWhenPersistFileToStorageFail() {
    ReportHistory reportHistory =
        ReportHistory.builder()
            .username("username")
            .reportName("reportName")
            .status(ReportStatus.IN_PROGRESS)
            .type(ReportType.STAKE_KEY)
            .id(10L)
            .build();
    PoolReportHistory poolReportHistory =
        spy(
            PoolReportHistory.builder()
                .reportHistory(reportHistory)
                .beginEpoch(0)
                .endEpoch(100)
                .eventDeregistration(Boolean.TRUE)
                .eventRegistration(Boolean.TRUE)
                .eventPoolUpdate(Boolean.TRUE)
                .eventReward(Boolean.TRUE)
                .isPoolSize(Boolean.TRUE)
                .build());

    when(poolReportRepository.findByReportHistoryId(anyLong()))
        .thenReturn(Optional.of(poolReportHistory));
    when(reportHistoryServiceAsync.exportInformationOnTheReport(any(PoolReportHistory.class)))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportPoolRegistration(poolReportHistory))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportPoolUpdate(poolReportHistory))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportRewardsDistribution(poolReportHistory))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportPoolDeregistration(poolReportHistory))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportEpochSize(poolReportHistory))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));

    when(excelHelper.writeContent(anyList(), isNull(), isNull()))
        .thenReturn(new ByteArrayInputStream(new byte[0]));
    doThrow(new RuntimeException()).when(storageService).uploadFile(any(), anyString());
    Assertions.assertThrows(
        Exception.class, () -> poolReportService.exportPoolReport(reportHistory.getId()));
    Assertions.assertEquals(ReportStatus.FAILED, poolReportHistory.getReportHistory().getStatus());
  }

  @Test
  void exportPoolReport_shouldSuccess() {
    ReportHistory reportHistory =
        ReportHistory.builder()
            .username("username")
            .reportName("reportName")
            .status(ReportStatus.IN_PROGRESS)
            .type(ReportType.STAKE_KEY)
            .id(10L)
            .build();
    PoolReportHistory poolReportHistory =
        spy(
            PoolReportHistory.builder()
                .reportHistory(reportHistory)
                .beginEpoch(0)
                .endEpoch(100)
                .eventDeregistration(Boolean.TRUE)
                .eventRegistration(Boolean.TRUE)
                .eventPoolUpdate(Boolean.TRUE)
                .eventReward(Boolean.TRUE)
                .isPoolSize(Boolean.TRUE)
                .build());
    when(poolReportRepository.findByReportHistoryId(anyLong()))
        .thenReturn(Optional.of(poolReportHistory));
    when(reportHistoryServiceAsync.exportInformationOnTheReport(any(PoolReportHistory.class)))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportPoolRegistration(poolReportHistory))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportPoolUpdate(poolReportHistory))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportRewardsDistribution(poolReportHistory))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportPoolDeregistration(poolReportHistory))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportEpochSize(poolReportHistory))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));

    when(excelHelper.writeContent(anyList(), isNull(), isNull()))
        .thenReturn(new ByteArrayInputStream(new byte[0]));
    doNothing().when(storageService).uploadFile(any(), anyString());
    when(poolReportRepository.save(any())).thenReturn(new PoolReportHistory());

    Assertions.assertDoesNotThrow(() -> poolReportService.exportPoolReport(reportHistory.getId()));
    Assertions.assertEquals(
        ReportStatus.GENERATED, poolReportHistory.getReportHistory().getStatus());
  }
}
