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
import org.cardanofoundation.job.repository.ledgersyncagg.AggregateAddressTokenRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AggregateAddressTxBalanceRepository;
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
  private final TxChartService txChartService;
  private final StakeAddressBalanceRepository stakeAddressBalanceRepository;
  private final TopAddressBalanceRepository topAddressBalanceRepository;
  private final TopStakeAddressBalanceRepository topStakeAddressBalanceRepository;
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
  public void refreshTopAddressBalance() {
    refreshMaterializedView(
        topAddressBalanceRepository::refreshMaterializedView, "TopAddressBalance");
  }

  @Scheduled(initialDelay = 50000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshTopStakeAddressBalance() {
    refreshMaterializedView(
        topStakeAddressBalanceRepository::refreshMaterializedView, "TopStakeAddressBalance");
  }

  @Scheduled(initialDelay = 20000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void refreshStakeAddressView() {
    refreshMaterializedView(
        stakeAddressBalanceRepository::refreshStakeAddressMaterializedView, "StakeAddressBalance");
  }

  @Scheduled(initialDelay = 50000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
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
