package org.cardanofoundation.job.schedules;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.repository.ledgersyncagg.AddressBalanceRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AggregateAddressTokenRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AggregateAddressTxBalanceRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.StakeAddressBalanceRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.StakeTxBalanceRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.TopAddressBalanceRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.TopStakeAddressBalanceRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.jooq.JOOQAddressBalanceRepository;
import org.cardanofoundation.job.service.TxChartService;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "jobs.agg-analytic.enabled",
    matchIfMissing = true,
    havingValue = "true")
public class AggregateAnalyticSchedule {

  private final AggregateAddressTokenRepository aggregateAddressTokenRepository;
  private final AggregateAddressTxBalanceRepository aggregateAddressTxBalanceRepository;
  private final TxChartService txChartService;
  private final JOOQAddressBalanceRepository jooqAddressBalanceRepository;
  private final AddressBalanceRepository addressBalanceRepository;
  private final StakeAddressBalanceRepository stakeAddressBalanceRepository;
  private final TopAddressBalanceRepository topAddressBalanceRepository;
  private final TopStakeAddressBalanceRepository topStakeAddressBalanceRepository;
  private final StakeTxBalanceRepository stakeTxBalanceRepository;

  @Value("${jobs.agg-analytic.number-of-concurrent-tasks}")
  private Integer numberOfConcurrentTasks;

  private final AtomicInteger currentConcurrentTasks = new AtomicInteger(0);

  @Scheduled(
      cron = "0 20 0 * * *",
      zone = "UTC") // midnight utc 0:20 AM make sure that it will not rollback to block has time <
  // midnight
  public void refreshAggBalanceAddressToken() {
    long currentTime = System.currentTimeMillis();
    log.info("---AggregateAddressTokenBalance--- Refresh job has been started");
    aggregateAddressTokenRepository.refreshMaterializedView();

    log.info(
        "---AggregateAddressTokenBalance--- Refresh job has ended. Time taken {}ms",
        System.currentTimeMillis() - currentTime);
  }

  @Scheduled(
      cron = "0 20 0 * * *",
      zone = "UTC") // midnight utc 0:20 AM make sure that it will not rollback to block has time <
  // midnight
  public void refreshAggBalanceAddressTx() {
    long currentTime = System.currentTimeMillis();
    log.info("---AggregateAddressTxBalance--- Refresh job has been started");
    aggregateAddressTxBalanceRepository.refreshMaterializedView();
    log.info(
        "---AggregateAddressTxBalance--- Refresh job has ended. Time taken {}ms",
        System.currentTimeMillis() - currentTime);
  }

  @Scheduled(initialDelay = 10000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void cleanUpAddressBalance() {
    long currentTime = System.currentTimeMillis();
    log.info("---CleanUpAddressBalance--- Remove old history record has been started");

    // Should be max slot - 43200 to ensure rollback case
    long targetSlot = addressBalanceRepository.getMaxSlot() - 43200;
    log.info("Cleaning address balance table. Target slot: {}", targetSlot);
    long totalDeletedRowsRows = 0;
    long deletedRows = 0;
    do {
      deletedRows = jooqAddressBalanceRepository.cleanUpAddressBalance(targetSlot, 1000);
      totalDeletedRowsRows += deletedRows;
      log.info("Total removed {} rows", totalDeletedRowsRows);
    } while (deletedRows > 0);

    log.info(
        "---CleanUpAddressBalance--- Remove history record has ended. Time taken {}ms",
        System.currentTimeMillis() - currentTime);
  }

  @Scheduled(initialDelay = 10000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshTopAddressBalance() {
    refreshMaterializedView(
        topAddressBalanceRepository::refreshMaterializedView, "TopAddressBalance");
  }

  @Scheduled(initialDelay = 30000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshTopStakeAddressBalance() {
    refreshMaterializedView(
        topStakeAddressBalanceRepository::refreshMaterializedView, "TopStakeAddressBalance");
  }

  @Scheduled(initialDelay = 20000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshStakeAddressView() {
    refreshMaterializedView(
        stakeAddressBalanceRepository::refreshStakeAddressMaterializedView, "StakeAddressView");
  }

  @Scheduled(initialDelay = 50000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void updateTxChartData() {
    refreshMaterializedView(txChartService::refreshDataForTxChart, "TxChartData");
  }

  @Scheduled(initialDelay = 60000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshStakeTxBalance() {
    refreshMaterializedView(stakeTxBalanceRepository::refreshMaterializedView, "StakeTxBalance");
  }

  public void refreshMaterializedView(Runnable refreshViewRunnable, String matViewName) {
    long currentTime = System.currentTimeMillis();
    log.info("---{}---- Refresh job has been started", matViewName);

    if (currentConcurrentTasks.get() < numberOfConcurrentTasks) {
      currentConcurrentTasks.incrementAndGet();
      refreshViewRunnable.run();
      currentConcurrentTasks.decrementAndGet();
    } else {
      log.info(
          "---{}---- Refresh job has been skipped due to full of concurrent tasks. Current concurrent tasks: {}",
          matViewName,
          currentConcurrentTasks);
      return;
    }

    log.info(
        "---{}---- Refresh job has ended. Time taken {}ms",
        matViewName,
        System.currentTimeMillis() - currentTime);
  }
}
