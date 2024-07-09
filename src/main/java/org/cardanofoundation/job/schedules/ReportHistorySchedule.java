package org.cardanofoundation.job.schedules;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.explorer.common.entity.enumeration.ReportStatus;
import org.cardanofoundation.explorer.common.entity.explorer.ReportHistory;
import org.cardanofoundation.job.repository.explorer.ReportHistoryRepository;
import org.cardanofoundation.job.service.impl.StorageReportServiceImpl;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Log4j2
@ConditionalOnProperty(
    value = "jobs.report-history.enabled",
    matchIfMissing = true,
    havingValue = "true")
public class ReportHistorySchedule {

  private final ReportHistoryRepository reportHistoryRepository;
  private final StorageReportServiceImpl storageService;

  /** Find all report history expired and delete from storage and set status to EXPIRED */
  @Scheduled(cron = "-")
  public void setExpiredReportHistory() {
    var currentTime = System.currentTimeMillis();
    Timestamp timeAt7dayAgo =
        Timestamp.valueOf(
            LocalDateTime.ofInstant(Instant.now().minus(Duration.ofDays(7)), ZoneOffset.UTC));

    List<ReportHistory> reportHistoryList =
        reportHistoryRepository.findNotExpiredReportHistoryByUploadedAtLessThan(timeAt7dayAgo);

    List<CompletableFuture<Void>> futures = new ArrayList<>();
    reportHistoryList.forEach(
        reportHistory ->
            futures.add(
                CompletableFuture.supplyAsync(
                    () -> {
                      storageService.deleteFile(reportHistory.getStorageKey());
                      reportHistory.setStatus(ReportStatus.EXPIRED);
                      return null;
                    })));
    futures.forEach(CompletableFuture::join);
    reportHistoryRepository.saveAllAndFlush(reportHistoryList);
    log.info(
        "Time taken to delete expired report history: {} ms",
        System.currentTimeMillis() - currentTime);
  }
}
