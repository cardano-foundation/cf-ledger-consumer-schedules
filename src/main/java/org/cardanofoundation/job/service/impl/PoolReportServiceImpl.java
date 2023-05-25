package org.cardanofoundation.job.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.stereotype.Service;

import org.cardanofoundation.explorer.consumercommon.entity.PoolReportHistory;
import org.cardanofoundation.explorer.consumercommon.enumeration.ReportStatus;
import org.cardanofoundation.job.common.enumeration.ExportType;
import org.cardanofoundation.job.repository.PoolReportHistoryRepository;
import org.cardanofoundation.job.service.PoolReportService;
import org.cardanofoundation.job.service.ReportHistoryServiceAsync;
import org.cardanofoundation.job.service.StorageService;
import org.cardanofoundation.job.util.report.ExcelHelper;
import org.cardanofoundation.job.util.report.ExportContent;

@Service
@Log4j2
@RequiredArgsConstructor
public class PoolReportServiceImpl implements PoolReportService {


  private final StorageService storageService;
  private final PoolReportHistoryRepository poolReportRepository;

  private final ReportHistoryServiceAsync reportHistoryServiceAsync;
  @Override
  public void exportPoolReport(PoolReportHistory poolReportHistory) throws Exception{
    var startTime = System.currentTimeMillis();
    try {
      log.info("Start generating report for {}", poolReportHistory.getReportHistory().getId());
      List<ExportContent> exportContents = getExportContents(poolReportHistory);
      String storageKey = generateStorageKey(poolReportHistory);
      String excelFileName = storageKey + ExportType.EXCEL.getValue();
      InputStream excelInputStream = ExcelHelper.writeContent(exportContents);
      storageService.uploadFile(excelInputStream.readAllBytes(), excelFileName);
      poolReportHistory.getReportHistory().setStatus(ReportStatus.GENERATED);
      poolReportHistory.getReportHistory().setStorageKey(storageKey);
      poolReportRepository.save(poolReportHistory);
    } catch (Exception e) {
      poolReportHistory.getReportHistory().setStatus(ReportStatus.FAILED);
      log.error("Error while generating report", e);
      throw e;
    } finally {
      poolReportRepository.save(poolReportHistory);
      var endTime = System.currentTimeMillis();
      log.info("Persist ReportHistory {} to storage time taken: {} ms",
               poolReportHistory.getReportHistory().getId(), endTime - startTime);

    }
  }

  private List<ExportContent> getExportContents(PoolReportHistory poolReportHistory) {
    List<CompletableFuture<ExportContent>> exportContents = new ArrayList<>();
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

    return exportContents.stream().map(CompletableFuture::join).toList();
  }




  private String generateStorageKey(PoolReportHistory poolReport) {
    return poolReport.getReportHistory().getId() + "_" + poolReport.getReportHistory()
        .getReportName();
  }
}
