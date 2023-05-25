package org.cardanofoundation.job.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.cardanofoundation.explorer.consumercommon.entity.StakeKeyReportHistory;
import org.cardanofoundation.explorer.consumercommon.enumeration.ReportStatus;
import org.cardanofoundation.job.common.enumeration.ExportType;
import org.cardanofoundation.job.dto.report.stake.StakeLifeCycleFilterRequest;
import org.cardanofoundation.job.repository.StakeKeyReportHistoryRepository;
import org.cardanofoundation.job.service.ReportHistoryServiceAsync;
import org.cardanofoundation.job.service.StakeKeyReportService;
import org.cardanofoundation.job.service.StorageService;
import org.cardanofoundation.job.util.report.ExcelHelper;
import org.cardanofoundation.job.util.report.ExportContent;

@Service
@Log4j2
@RequiredArgsConstructor
public class StakeKeyReportServiceImpl implements StakeKeyReportService {

  private final StorageService storageService;
  private final StakeKeyReportHistoryRepository stakeKeyReportHistoryRepository;
  private final ReportHistoryServiceAsync reportHistoryServiceAsync;

  @Override
  public void exportStakeKeyReport(StakeKeyReportHistory stakeKeyReportHistory) throws Exception {
    var startTime = System.currentTimeMillis();
    try {
      log.info("Start generating report for ReportHistory {}",
               stakeKeyReportHistory.getReportHistory().getId());
      List<ExportContent> exportContents = getExportContents(stakeKeyReportHistory);
      String storageKey = generateStorageKey(stakeKeyReportHistory);
      String excelFileName = storageKey + ExportType.EXCEL.getValue();
      InputStream excelInputStream = ExcelHelper.writeContent(exportContents);
      storageService.uploadFile(excelInputStream.readAllBytes(), excelFileName);
      stakeKeyReportHistory.getReportHistory().setStatus(ReportStatus.GENERATED);
      stakeKeyReportHistory.getReportHistory().setStorageKey(storageKey);
    } catch (Exception e) {
      stakeKeyReportHistory.getReportHistory().setStatus(ReportStatus.FAILED);
      log.error("Error while generating report", e);
      throw e;
    } finally {
      stakeKeyReportHistoryRepository.save(stakeKeyReportHistory);
      var endTime = System.currentTimeMillis();
      log.info("Persist ReportHistory {} to storage time taken: {} ms",
               stakeKeyReportHistory.getReportHistory().getId(), endTime - startTime);
    }
  }

  private List<ExportContent> getExportContents(StakeKeyReportHistory stakeKeyReportHistory) {
    StakeLifeCycleFilterRequest stakeLifeCycleFilterRequest = getStakeLifeCycleFilterRequest(
        stakeKeyReportHistory);
    var currentTime = System.currentTimeMillis();
    List<CompletableFuture<ExportContent>> exportContents = new ArrayList<>();

    if (Boolean.TRUE.equals(stakeKeyReportHistory.getEventRegistration())) {
      exportContents.add(
          reportHistoryServiceAsync.exportStakeRegistrations(stakeKeyReportHistory.getStakeKey(),
                                                             stakeLifeCycleFilterRequest));
    }

    if (Boolean.TRUE.equals(stakeKeyReportHistory.getEventDelegation())) {
      exportContents.add(
          reportHistoryServiceAsync.exportStakeDelegations(stakeKeyReportHistory.getStakeKey(),
                                                           stakeLifeCycleFilterRequest));
    }

    if (Boolean.TRUE.equals(stakeKeyReportHistory.getEventRewards())) {
      exportContents.add(
          reportHistoryServiceAsync.exportStakeRewards(stakeKeyReportHistory.getStakeKey(),
                                                       stakeLifeCycleFilterRequest));
    }

    if (Boolean.TRUE.equals(stakeKeyReportHistory.getEventWithdrawal())) {
      exportContents.add(
          reportHistoryServiceAsync.exportStakeWithdrawals(stakeKeyReportHistory.getStakeKey(),
                                                           stakeLifeCycleFilterRequest));
    }

    if (Boolean.TRUE.equals(stakeKeyReportHistory.getEventDeregistration())) {
      exportContents.add(
          reportHistoryServiceAsync.exportStakeDeregistrations(stakeKeyReportHistory.getStakeKey(),
                                                               stakeLifeCycleFilterRequest));
    }

    if (Boolean.TRUE.equals(stakeKeyReportHistory.getIsADATransfer())) {
      exportContents.add(
          reportHistoryServiceAsync.exportStakeWalletActivitys(
              stakeKeyReportHistory.getStakeKey(), stakeKeyReportHistory.getIsFeesPaid(),
              stakeLifeCycleFilterRequest));
    }

    var response = exportContents.stream().map(CompletableFuture::join).toList();
    log.info("Get all stake key report data time taken: {} ms",
             System.currentTimeMillis() - currentTime);
    return response;
  }

  private String generateStorageKey(StakeKeyReportHistory stakeKeyReportHistory) {
    return stakeKeyReportHistory.getReportHistory().getId() + "_"
        + stakeKeyReportHistory.getReportHistory()
        .getReportName();
  }

  private StakeLifeCycleFilterRequest getStakeLifeCycleFilterRequest(
      StakeKeyReportHistory stakeKeyReportHistory) {
    return StakeLifeCycleFilterRequest.builder()
        .fromDate(stakeKeyReportHistory.getFromDate())
        .toDate(stakeKeyReportHistory.getToDate())
        .build();
  }
}
