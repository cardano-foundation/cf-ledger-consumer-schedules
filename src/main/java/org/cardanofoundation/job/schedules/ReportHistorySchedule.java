package org.cardanofoundation.job.schedules;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.cardanofoundation.explorer.common.entity.enumeration.ReportStatus;
import org.cardanofoundation.explorer.common.entity.explorer.ReportHistory;
import org.cardanofoundation.job.repository.explorer.ReportHistoryRepository;
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

  private static final Logger logger = LogManager.getLogger(ReportHistorySchedule.class);
  private final ReportHistoryRepository reportHistoryRepository;
  private final StorageReportServiceImpl storageService;
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

  @Scheduled(fixedRateString = "${jobs.report-history.fixed-delay}")
  public void consumeReport() {
    var currentTime = System.currentTimeMillis();
    AtomicLong successProcess = new AtomicLong(0);
    List<ReportHistory> reportHistories =
        reportHistoryRepository.findAllReportHistoryByStatus(ReportStatus.IN_PROGRESS);
    reportHistories.parallelStream()
        .forEach(
            reportHistory -> {
              switch (reportHistory.getType()) {
                case STAKE_KEY:
                  try {
                    var time = System.currentTimeMillis();
                    stakeKeyReportService.exportStakeKeyReport(reportHistory.getId());
                    logger.info(
                        "Time taken to generate stake key report with id {}: {} ms",
                        reportHistory.getId(),
                        System.currentTimeMillis() - time);
                  } catch (Exception e) {
                    logger.error(
                        "Consuming report history with ID {} failed: {}",
                        reportHistory.getId(),
                        e.getMessage());
                  } finally {
                    successProcess.incrementAndGet();
                  }
                  break;
                case POOL_ID:
                  try {
                    var time = System.currentTimeMillis();
                    poolReportService.exportPoolReport(reportHistory.getId());
                    logger.info(
                        "Time taken to generate pool report with id {}: {} ms",
                        reportHistory.getId(),
                        System.currentTimeMillis() - time);
                  } catch (Exception e) {
                    logger.error(
                        "Consuming report history with ID {} failed: {}",
                        reportHistory.getId(),
                        e.getMessage());
                  } finally {
                    successProcess.incrementAndGet();
                  }
                  break;
                default:
                  break;
              }
            });
    logger.info(
        "Consumed {} reports: {} succeeded , {} failed and taking {} ms",
        reportHistories.size(),
        successProcess.get(),
        reportHistories.size() - successProcess.get(),
        System.currentTimeMillis() - currentTime);
  }
}
