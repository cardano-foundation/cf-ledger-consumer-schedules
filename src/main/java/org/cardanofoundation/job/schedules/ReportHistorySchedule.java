package org.cardanofoundation.job.schedules;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.explorer.consumercommon.entity.ReportHistory;
import org.cardanofoundation.explorer.consumercommon.enumeration.ReportStatus;
import org.cardanofoundation.job.repository.ReportHistoryRepository;
import org.cardanofoundation.job.service.StorageService;


@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Log4j2
@ConditionalOnProperty(value = "jobs.report-history.enabled", matchIfMissing = true, havingValue = "true")
public class ReportHistorySchedule {

  private final ReportHistoryRepository reportHistoryRepository;
  private final StorageService storageService;

  @Value("${jobs.report-history.expired.rate}")
  private long expiredFixedRate;


  /**
   * Find all report history expired and delete from storage
   */
  @Scheduled(fixedRateString = "${jobs.report-history.expired.rate}", initialDelay = 3000)
  void setExpiredReportHistory() {
    var currentTime = System.currentTimeMillis();
    Timestamp timestamp = new Timestamp(currentTime - expiredFixedRate);
    List<ReportHistory> reportHistoryList = reportHistoryRepository.findByUploadedAtLessThan(
        timestamp);


    reportHistoryList.forEach(reportHistory -> {
//      reportHistory.setStatus(ReportStatus.EXPIRED);
      storageService.deleteFile(reportHistory.getStorageKey());
    });
    reportHistoryRepository.saveAllAndFlush(reportHistoryList);
    log.info("Time taken to delete expired report history: {} ms",
             System.currentTimeMillis() - currentTime);
  }



}
