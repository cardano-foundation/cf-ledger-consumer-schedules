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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import org.cardanofoundation.explorer.consumercommon.entity.StakeKeyReportHistory;
import org.cardanofoundation.explorer.consumercommon.enumeration.ReportStatus;
import org.cardanofoundation.job.common.enumeration.ExportType;
import org.cardanofoundation.job.dto.report.stake.StakeLifeCycleFilterRequest;
import org.cardanofoundation.job.repository.AddressTxBalanceRepository;
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

  @Autowired
  @Qualifier("storageReportServiceImpl")
  private StorageService storageService;
  private final StakeKeyReportHistoryRepository stakeKeyReportHistoryRepository;
  private final ReportHistoryServiceAsync reportHistoryServiceAsync;
  private final AddressTxBalanceRepository addressTxBalanceRepository;
  private final ExcelHelper excelHelper;

  @Value("${jobs.limit-content}")
  private int limitSize;

  @Value("${application.network}")
  private String folderPrefix;
  /**
   * Export stake key report
   *
   * @param stakeKeyReportHistory stake key report history
   * @throws Exception
   */
  @Override
  public void exportStakeKeyReport(StakeKeyReportHistory stakeKeyReportHistory) throws Exception {
    var startTime = System.currentTimeMillis();
    try {
      List<ExportContent> exportContents = getExportContents(stakeKeyReportHistory);
      String storageKey = generateStorageKey(stakeKeyReportHistory);
      String excelFileName = storageKey + ExportType.EXCEL.getValue();
      InputStream excelInputStream = excelHelper.writeContent(exportContents);
      storageService.uploadFile(excelInputStream.readAllBytes(), excelFileName);
      stakeKeyReportHistory.getReportHistory().setStatus(ReportStatus.GENERATED);
      stakeKeyReportHistory.getReportHistory().setStorageKey(storageKey);
      stakeKeyReportHistory
          .getReportHistory()
          .setUploadedAt(Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)));
    } catch (Exception e) {
      stakeKeyReportHistory.getReportHistory().setStatus(ReportStatus.FAILED);
      log.error("Error while generating report", e);
      throw e;
    } finally {
      stakeKeyReportHistoryRepository.save(stakeKeyReportHistory);
      var endTime = System.currentTimeMillis();
      log.info(
          "Persist ReportHistory {} to storage time taken: {} ms",
          stakeKeyReportHistory.getReportHistory().getId(),
          endTime - startTime);
    }
  }

  /**
   * Get ExportContent correspondingly to each event
   *
   * @param stakeKeyReportHistory stakeKeyReportHistory
   * @return List<ExportContent>
   */
  private List<ExportContent> getExportContents(StakeKeyReportHistory stakeKeyReportHistory) {
    StakeLifeCycleFilterRequest stakeLifeCycleFilterRequest =
        getStakeLifeCycleFilterRequest(stakeKeyReportHistory);

    var currentTime = System.currentTimeMillis();
    List<CompletableFuture<ExportContent>> exportContents = new ArrayList<>();

    /* Check all events are enabled or not then get content correspondingly to each event
     * Each data of event will be stored in a different sheet.
     * Due to retrieving data for each sheet is independent of one another, so we can use CompletableFuture
     * to retrieve data concurrently.
     * ReportHistoryServiceAsync is used to retrieve data concurrently.
     */

    if (Boolean.TRUE.equals(stakeKeyReportHistory.getEventRegistration())) {
      exportContents.add(
          reportHistoryServiceAsync.exportStakeRegistrations(
              stakeKeyReportHistory.getStakeKey(), stakeLifeCycleFilterRequest));
    }

    if (Boolean.TRUE.equals(stakeKeyReportHistory.getEventDelegation())) {
      exportContents.add(
          reportHistoryServiceAsync.exportStakeDelegations(
              stakeKeyReportHistory.getStakeKey(), stakeLifeCycleFilterRequest));
    }

    if (Boolean.TRUE.equals(stakeKeyReportHistory.getEventRewards())) {
      exportContents.add(
          reportHistoryServiceAsync.exportStakeRewards(
              stakeKeyReportHistory.getStakeKey(), stakeLifeCycleFilterRequest));
    }

    if (Boolean.TRUE.equals(stakeKeyReportHistory.getEventWithdrawal())) {
      exportContents.add(
          reportHistoryServiceAsync.exportStakeWithdrawals(
              stakeKeyReportHistory.getStakeKey(), stakeLifeCycleFilterRequest));
    }

    if (Boolean.TRUE.equals(stakeKeyReportHistory.getEventDeregistration())) {
      exportContents.add(
          reportHistoryServiceAsync.exportStakeDeregistrations(
              stakeKeyReportHistory.getStakeKey(), stakeLifeCycleFilterRequest));
    }

    if (Boolean.TRUE.equals(stakeKeyReportHistory.getIsADATransfer())) {
      // Due to the limitation of Excel sheet is 1,048,576 rows and ada transfer may have a huge
      // amount of data,
      // So we need to split the content in to multiple sheets

      // Get total content
      Long totalContent =
          addressTxBalanceRepository.getCountTxByStakeInDateRange(
              stakeKeyReportHistory.getStakeKey(),
              stakeLifeCycleFilterRequest.getFromDate(),
              stakeLifeCycleFilterRequest.getToDate());

      // Split content into multiple sheets
      long totalPage = totalContent / limitSize;
      for (int i = 0; i <= totalPage; i++) {
        String subTitle = "_" + i * limitSize + "_" + (i + 1) * limitSize;
        if (i == 0 && totalPage == 0) {
          subTitle = "";
        }
        Pageable pageable = PageRequest.of(i, limitSize, Sort.by("time").descending());
        exportContents.add(
            reportHistoryServiceAsync.exportStakeWalletActivitys(
                stakeKeyReportHistory.getStakeKey(),
                stakeLifeCycleFilterRequest,
                pageable,
                subTitle));
      }
    }

    var response = exportContents.stream().map(CompletableFuture::join).toList();
    log.info(
        "Get all stake key report data time taken: {} ms",
        System.currentTimeMillis() - currentTime);
    return response;
  }

  /**
   * Generate storage key for report storage key = report_history_id + report_name
   *
   * @param stakeKeyReportHistory
   * @return storage_key
   */
  private String generateStorageKey(StakeKeyReportHistory stakeKeyReportHistory) {
    return folderPrefix + "/" + stakeKeyReportHistory.getReportHistory().getId()
        + "_"
        + stakeKeyReportHistory.getReportHistory().getReportName();
  }

  /**
   * Get filter condition
   *
   * @param stakeKeyReportHistory
   * @return condition
   */
  private StakeLifeCycleFilterRequest getStakeLifeCycleFilterRequest(
      StakeKeyReportHistory stakeKeyReportHistory) {
    return StakeLifeCycleFilterRequest.builder()
        .fromDate(stakeKeyReportHistory.getFromDate())
        .toDate(stakeKeyReportHistory.getToDate())
        .build();
  }
}
