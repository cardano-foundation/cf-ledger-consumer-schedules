package org.cardanofoundation.job.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.repository.ledgersync.AddressTxCountRepository;
import org.cardanofoundation.job.repository.ledgersync.LatestAddressBalanceRepository;
import org.cardanofoundation.job.repository.ledgersync.LatestStakeAddressBalanceRepository;
import org.cardanofoundation.job.repository.ledgersync.LatestTokenBalanceRepository;
import org.cardanofoundation.job.repository.ledgersync.StakeAddressTxCountRepository;
import org.cardanofoundation.job.repository.ledgersync.aggregate.AggregateAddressTokenRepository;
import org.cardanofoundation.job.repository.ledgersync.aggregate.AggregateAddressTxBalanceRepository;
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
  private final LatestTokenBalanceRepository latestTokenBalanceRepository;
  private final LatestAddressBalanceRepository latestAddressBalanceRepository;
  private final LatestStakeAddressBalanceRepository latestStakeAddressBalanceRepository;
  private final AddressTxCountRepository addressTxCountRepository;
  private final StakeAddressTxCountRepository stakeAddressTxCountRepository;
  private final TxChartService txChartService;

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

  @Scheduled(initialDelay = 10800000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshLatestTokenBalance() {
    long currentTime = System.currentTimeMillis();
    log.info("---LatestTokenBalance--- Refresh job has been started");
    latestTokenBalanceRepository.refreshMaterializedView();
    log.info(
        "LatestTokenBalance - Refresh job has ended. Time taken {} ms",
        System.currentTimeMillis() - currentTime);
  }

  @Scheduled(initialDelay = 10800000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshLatestAddressBalance() {
    long currentTime = System.currentTimeMillis();
    log.info("---LatestAddressBalance--- - Refresh job has been started");
    latestAddressBalanceRepository.refreshMaterializedView();
    log.info(
        "LatestAddressBalance - Refresh job ended. Time taken {} ms",
        System.currentTimeMillis() - currentTime);
  }

  @Scheduled(initialDelay = 7200000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshLatestStakeAddressBalance() {
    long currentTime = System.currentTimeMillis();
    log.info("---LatestStakeAddressBalance--- Refresh job has been started");
    latestStakeAddressBalanceRepository.refreshMaterializedView();
    log.info(
        "---LatestStakeAddressBalance--- Refresh job has ended. Time taken {} ms",
        System.currentTimeMillis() - currentTime);
  }

  @Scheduled(initialDelay = 7200000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshLatestStakeAddressTxCount() {
    long currentTime = System.currentTimeMillis();
    log.info("---LatestStakeAddressTxCount--- Refresh job has been started");
    stakeAddressTxCountRepository.refreshMaterializedView();
    log.info(
        "---LatestStakeAddressTxCount--- Refresh job has ended. Time taken {} ms",
        System.currentTimeMillis() - currentTime);
  }

  @Scheduled(initialDelay = 7200000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void updateTxCountTable() {
    log.info("---LatestAddressTxCount--- Refresh job has been started");
    long startTime = System.currentTimeMillis();
    addressTxCountRepository.refreshMaterializedView();
    long executionTime = System.currentTimeMillis() - startTime;
    log.info("---LatestAddressTxCount--- Refresh job has ended. Time taken {} ms", executionTime);
  }

  @Scheduled(fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void updateTxChartData() {
    log.info("---TxChart--- Refresh job has been started");
    long startTime = System.currentTimeMillis();
    txChartService.refreshDataForTxChart();
    log.info(
        "---TxChart--- Refresh job has ended. Time taken {} ms",
        System.currentTimeMillis() - startTime);
  }
}
