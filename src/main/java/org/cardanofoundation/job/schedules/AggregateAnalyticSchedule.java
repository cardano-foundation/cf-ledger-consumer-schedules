package org.cardanofoundation.job.schedules;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.repository.ledgersync.AddressTxCountRepository;
import org.cardanofoundation.job.repository.ledgersync.LatestAddressBalanceRepository;
import org.cardanofoundation.job.repository.ledgersync.LatestStakeAddressBalanceRepository;
import org.cardanofoundation.job.repository.ledgersync.LatestTokenBalanceRepository;
import org.cardanofoundation.job.repository.ledgersync.StakeAddressTxCountRepository;
import org.cardanofoundation.job.repository.ledgersync.TokenTxCountRepository;
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
  private final TokenTxCountRepository tokenTxCountRepository;
  private final TxChartService txChartService;

  private final RedisTemplate<String, Integer> redisTemplate;

  @Value("${application.network}")
  private String network;

  @Value("${jobs.agg-analytic.number-of-concurrent-tasks}")
  private Integer numberOfConcurrentTasks;

  @PostConstruct
  public void init() {
    redisTemplate.delete(getRedisKey(RedisKey.AGGREGATED_CONCURRENT_TASKS_COUNT.name()));
  }

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

  @Scheduled(initialDelay = 40000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshLatestAddressBalance() {
    refreshMaterializedView(
        latestAddressBalanceRepository::refreshMaterializedView, "LatestAddressBalance");
  }

  @Scheduled(initialDelay = 50000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshLatestStakeAddressBalance() {
    refreshMaterializedView(
        latestStakeAddressBalanceRepository::refreshMaterializedView, "LatestStakeAddressBalance");
  }

  @Scheduled(initialDelay = 30000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void updateTxChartData() {
    refreshMaterializedView(txChartService::refreshDataForTxChart, "TxChartData");
  }

  private String getRedisKey(String prefix) {
    return prefix + "_" + network;
  }

  public void refreshMaterializedView(Runnable refreshViewRunnable, String matViewName) {
    long currentTime = System.currentTimeMillis();
    log.info("---{}---- Refresh job has been started", matViewName);
    String concurrentTasksKey = getRedisKey(RedisKey.AGGREGATED_CONCURRENT_TASKS_COUNT.name());
    Integer currentConcurrentTasks = redisTemplate.opsForValue().get(concurrentTasksKey);

    if (currentConcurrentTasks == null || currentConcurrentTasks < numberOfConcurrentTasks) {
      redisTemplate
          .opsForValue()
          .set(concurrentTasksKey, currentConcurrentTasks == null ? 1 : currentConcurrentTasks + 1);
      refreshViewRunnable.run();
      redisTemplate
          .opsForValue()
          .decrement(getRedisKey(RedisKey.AGGREGATED_CONCURRENT_TASKS_COUNT.name()));
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
