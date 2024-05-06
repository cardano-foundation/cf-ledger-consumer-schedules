package org.cardanofoundation.job.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.repository.ledgersync.AddressTxCountRepository;
import org.cardanofoundation.job.repository.ledgersync.LatestTokenBalanceRepository;
import org.cardanofoundation.job.repository.ledgersync.aggregate.AggregateAddressTokenRepository;
import org.cardanofoundation.job.repository.ledgersync.aggregate.AggregateAddressTxBalanceRepository;

@Component
@Slf4j
@RequiredArgsConstructor
public class AggregateAnalyticSchedule {

  private final AggregateAddressTokenRepository aggregateAddressTokenRepository;
  private final AggregateAddressTxBalanceRepository aggregateAddressTxBalanceRepository;
  private final LatestTokenBalanceRepository latestTokenBalanceRepository;
  private final AddressTxCountRepository addressTxCountRepository;

  @Scheduled(
      cron = "0 20 0 * * *",
      zone = "UTC") // midnight utc 0:20 AM make sure that it will not rollback to block has time <
  // midnight
  public void refreshAggBalanceAddressToken() {
    long currentTime = System.currentTimeMillis();
    log.info("Start job refreshAggBalanceAddressToken");
    aggregateAddressTokenRepository.refreshMaterializedView();

    log.info(
        "End Job refreshAggBalanceAddressToken, Time taken {}ms",
        System.currentTimeMillis() - currentTime);
  }

  @Scheduled(
      cron = "0 20 0 * * *",
      zone = "UTC") // midnight utc 0:20 AM make sure that it will not rollback to block has time <
  // midnight
  public void refreshAggBalanceAddressTx() {
    long currentTime = System.currentTimeMillis();
    log.info("Start job refreshAggBalanceAddressTx");
    aggregateAddressTxBalanceRepository.refreshMaterializedView();
    log.info(
        "End Job refreshAggBalanceAddressTx, Time taken {}ms",
        System.currentTimeMillis() - currentTime);
  }

  @Scheduled(fixedDelay = 1000 * 60 * 5) // 5 minutes
  public void refreshLatestTokenBalance() {
    long currentTime = System.currentTimeMillis();
    log.info("Start job refreshLatestTokenBalance");
    latestTokenBalanceRepository.refreshMaterializedView();
    log.info(
        "End Job refreshLatestTokenBalance, Time taken {}ms",
        System.currentTimeMillis() - currentTime);
  }

  @Scheduled(fixedRateString = "${jobs.agg-address.fixed-delay}")
  public void updateTxCountTable() {
    log.info("Start job to update tx count for address");
    long startTime = System.currentTimeMillis();
    addressTxCountRepository.refreshMaterializedView();
    long executionTime = System.currentTimeMillis() - startTime;
    log.info("Update tx count for address successfully, takes: [{} ms]", executionTime);
  }
}
