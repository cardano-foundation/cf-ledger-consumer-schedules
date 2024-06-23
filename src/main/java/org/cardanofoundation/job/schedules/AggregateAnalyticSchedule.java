package org.cardanofoundation.job.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.repository.ledgersyncagg.AggregateAddressTokenRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AggregateAddressTxBalanceRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.LatestTokenBalanceRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.StakeAddressBalanceRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.TopAddressBalanceRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.TopStakeAddressBalanceRepository;
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
  private final TxChartService txChartService;
  private final StakeAddressBalanceRepository stakeAddressBalanceRepository;
  private final TopAddressBalanceRepository topAddressBalanceRepository;
  private final TopStakeAddressBalanceRepository topStakeAddressBalanceRepository;

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

  @Scheduled(initialDelay = 3600000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshLatestTokenBalance() {
    long currentTime = System.currentTimeMillis();
    log.info("---LatestTokenBalance--- Refresh job has been started");
    latestTokenBalanceRepository.refreshMaterializedView();
    log.info(
        "LatestTokenBalance - Refresh job has ended. Time taken {} ms",
        System.currentTimeMillis() - currentTime);
  }

  @Scheduled(initialDelay = 7200000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshTopAddressBalance() {
    long currentTime = System.currentTimeMillis();
    log.info("---Top1000AddressBalance--- - Refresh job has been started");
    topAddressBalanceRepository.refreshMaterializedView();
    log.info(
        "Top1000AddressBalance - Refresh job ended. Time taken {} ms",
        System.currentTimeMillis() - currentTime);
  }

  @Scheduled(initialDelay = 10800000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshTopStakeAddressBalance() {
    long currentTime = System.currentTimeMillis();
    log.info("---Top1000StakeAddressBalance--- Refresh job has been started");
    topStakeAddressBalanceRepository.refreshMaterializedView();
    log.info(
        "---Top1000StakeAddressBalance--- Refresh job has ended. Time taken {} ms",
        System.currentTimeMillis() - currentTime);
  }

  @Scheduled(initialDelay = 200000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshStakeAddressView() {
    long currentTime = System.currentTimeMillis();
    log.info("---StakeAddressView--- Refresh job has been started");
    stakeAddressBalanceRepository.refreshStakeAddressMaterializedView();
    log.info(
        "---StakeAddressView--- Refresh job has ended. Time taken {} ms",
        System.currentTimeMillis() - currentTime);
  }

  @Scheduled(initialDelay = 50000,fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void updateTxChartData() {
    log.info("---TxChart--- Refresh job has been started");
    long startTime = System.currentTimeMillis();
    txChartService.refreshDataForTxChart();
    log.info(
        "---TxChart--- Refresh job has ended. Time taken {} ms",
        System.currentTimeMillis() - startTime);
  }
}
