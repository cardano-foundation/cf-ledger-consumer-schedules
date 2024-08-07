package org.cardanofoundation.job.service.impl;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.cardanofoundation.explorer.common.entity.enumeration.ReportStatus;
import org.cardanofoundation.explorer.common.entity.explorer.PoolReportHistory;
import org.cardanofoundation.job.common.enumeration.ExportType;
import org.cardanofoundation.job.repository.explorer.PoolReportHistoryRepository;
import org.cardanofoundation.job.service.PoolReportService;
import org.cardanofoundation.job.service.ReportHistoryServiceAsync;
import org.cardanofoundation.job.util.report.ExcelHelper;
import org.cardanofoundation.job.util.report.ExportContent;

@Service
@Log4j2
@RequiredArgsConstructor
public class PoolReportServiceImpl implements PoolReportService {

  private final StorageReportServiceImpl storageService;
  private final PoolReportHistoryRepository poolReportRepository;
  private final ExcelHelper excelHelper;
  private final ReportHistoryServiceAsync reportHistoryServiceAsync;

  @Value("${application.network}")
  private String folderPrefix;

  @Override
  public void exportPoolReport(Long reportId) throws Exception {
    PoolReportHistory poolReportHistory =
        poolReportRepository
            .findByReportHistoryId(reportId)
            .orElseThrow(() -> new RuntimeException("Report not found"));
    try {
      Long zoneOffset = poolReportHistory.getReportHistory().getZoneOffset();
      String timePattern = poolReportHistory.getReportHistory().getTimePattern();
      List<ExportContent> exportContents = getExportContents(poolReportHistory);
      String storageKey = generateStorageKey(poolReportHistory);
      String excelFileName = storageKey + ExportType.EXCEL.getValue();
      InputStream excelInputStream =
          excelHelper.writeContent(exportContents, zoneOffset, timePattern);
      storageService.uploadFile(excelInputStream.readAllBytes(), excelFileName);
      poolReportHistory.getReportHistory().setStatus(ReportStatus.GENERATED);
      poolReportHistory.getReportHistory().setStorageKey(storageKey);
      poolReportHistory
          .getReportHistory()
          .setUploadedAt(Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)));
    } catch (Exception e) {
      poolReportHistory.getReportHistory().setStatus(ReportStatus.FAILED);
      log.error("Error while generating report", e);
      throw e;
    } finally {
      poolReportRepository.save(poolReportHistory);
    }
  }

  private List<ExportContent> getExportContents(PoolReportHistory poolReportHistory) {
    List<CompletableFuture<ExportContent>> exportContents = new ArrayList<>();
    /* Check all events are enabled or not then get content correspondingly to each event
     * Each data of event will be stored in a different sheet.
     * Due to retrieving data for each sheet is independent of one another,
     * so we can use CompletableFuture
     * to retrieve data concurrently.
     * ReportHistoryServiceAsync is used to retrieve data concurrently.
     */
    exportContents.add(reportHistoryServiceAsync.exportInformationOnTheReport(poolReportHistory));

    if (Boolean.TRUE.equals(poolReportHistory.getEventRegistration())) {
      exportContents.add(reportHistoryServiceAsync.exportPoolRegistration(poolReportHistory));
    }
    if (Boolean.TRUE.equals(poolReportHistory.getEventPoolUpdate())) {
      exportContents.add(reportHistoryServiceAsync.exportPoolUpdate(poolReportHistory));
    }
    if (Boolean.TRUE.equals(poolReportHistory.getEventReward())) {
      exportContents.add(reportHistoryServiceAsync.exportRewardsDistribution(poolReportHistory));
    }
    if (Boolean.TRUE.equals(poolReportHistory.getEventDeregistration())) {
      exportContents.add(reportHistoryServiceAsync.exportPoolDeregistration(poolReportHistory));
    }
    if (Boolean.TRUE.equals(poolReportHistory.getIsPoolSize())) {
      exportContents.add(reportHistoryServiceAsync.exportEpochSize(poolReportHistory));
    }
    var response = exportContents.stream().map(CompletableFuture::join).toList();
    return response;
  }

  /**
   * Generate storage key for report storage key = report_history_id + report_name
   *
   * @param poolReport PoolReportHistory
   * @return storage key
   */
  private String generateStorageKey(PoolReportHistory poolReport) {
    return folderPrefix
        + "/"
        + poolReport.getReportHistory().getId()
        + "_"
        + poolReport.getReportHistory().getReportName();
  }
}
