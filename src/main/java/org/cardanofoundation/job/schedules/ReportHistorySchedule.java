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
import org.cardanofoundation.explorer.common.entity.explorer.PoolReportHistory;
import org.cardanofoundation.explorer.common.entity.explorer.ReportHistory;
import org.cardanofoundation.explorer.common.entity.explorer.StakeKeyReportHistory;
import org.cardanofoundation.job.repository.explorer.PoolReportHistoryRepository;
import org.cardanofoundation.job.repository.explorer.ReportHistoryRepository;
import org.cardanofoundation.job.repository.explorer.StakeKeyReportHistoryRepository;
import org.cardanofoundation.job.service.PoolReportService;
import org.cardanofoundation.job.service.StakeKeyReportService;
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
  private final StakeKeyReportHistoryRepository stakeKeyReportHistoryRepository;
  private final PoolReportHistoryRepository poolReportHistoryRepository;
  private final StakeKeyReportService stakeKeyReportService;
  private final PoolReportService poolReportService;

  /** Find all report history expired and delete from storage and set status to EXPIRED */
  @Scheduled(fixedRateString = "${jobs.report-history.expired.rate}", initialDelay = 3000)
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

  @Scheduled(cron = "3 * * * * *", initialDelay = 3000)
  public void consume() {
    var currentTime = System.currentTimeMillis();
    try {
      List<ReportHistory> reportHistories =
          reportHistoryRepository.findAllReportHistoryByStatus(ReportStatus.IN_PROGRESS);

      reportHistories.forEach(
          reportHistory -> {
            switch (reportHistory.getType()) {
              case STAKE_KEY:
                StakeKeyReportHistory stakeKeyReportHistory =
                    stakeKeyReportHistoryRepository.findByReportHistoryId(reportHistory.getId());
                stakeKeyReportService.exportStakeKeyReport(
                    stakeKeyReportHistory,
                    reportHistory.getZoneOffset(),
                    reportHistory.getTimePattern(),
                    reportHistory.getDateFormat());
                break;
              case POOL_ID:
                PoolReportHistory poolReportHistory =
                    poolReportHistoryRepository.findByReportHistoryId(reportHistory.getId());
                poolReportService.exportPoolReport(
                    poolReportHistory,
                    reportHistory.getZoneOffset(),
                    reportHistory.getTimePattern(),
                    reportHistory.getDateFormat());
                break;
              default:
                break;
            }
          });
    } catch (Exception e) {
      log.error("Consume report history failure: {}", e.getMessage());
    }

    log.info(
        "End job consume report history: {} ms",
        System.currentTimeMillis() - currentTime);
  }
}
