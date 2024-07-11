package org.cardanofoundation.job.schedules;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;

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
import org.cardanofoundation.job.repository.ledgersyncagg.jooq.JOOQStakeAddressBalanceRepository;
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
  private final JOOQStakeAddressBalanceRepository jooqStakeAddressBalanceRepository;
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
    cleanUpBalance(
        jooqAddressBalanceRepository::cleanUpAddressBalance,
        addressBalanceRepository::getMaxSlot,
        "AddressBalance");
  }

  @Scheduled(initialDelay = 10000, fixedDelayString = "${jobs.agg-analytic.fixed-delay}")
  public void cleanUpStakeAddressBalance() {
    cleanUpBalance(
        jooqStakeAddressBalanceRepository::cleanUpStakeAddressBalance,
        stakeAddressBalanceRepository::getMaxSlot,
        "StakeAddressBalance");
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

  private void cleanUpBalance(
      BiFunction<Long, Integer, Integer> cleanUpFunction,
      Supplier<Long> maxSlotSupplier,
      String tableName) {
    long currentTime = System.currentTimeMillis();
    log.info("---CleanUp{}---- Remove history record has been started", tableName);

    // Should be max slot - 43200 to ensure rollback case
    long targetSlot = maxSlotSupplier.get() - 43200;
    log.info("Cleaning {} table. Target slot: {}", tableName, targetSlot);
    long totalDeletedRowsRows = 0;
    long deletedRows = 0;
    final int fixedBatchSize = 10;
    final int deletedRowsThreshold = 10000;

    List<CompletableFuture<Integer>> cleanUpFutures = new ArrayList<>();
    do {

      for (int i = 0; i < fixedBatchSize; i++) {
        CompletableFuture<Integer> future =
            CompletableFuture.supplyAsync(
                () -> cleanUpFunction.apply(targetSlot, deletedRowsThreshold));
        cleanUpFutures.add(future);
      }

      CompletableFuture.allOf(cleanUpFutures.toArray(new CompletableFuture[0])).join();

      deletedRows = cleanUpFutures.stream().mapToInt(CompletableFuture::join).sum();
      totalDeletedRowsRows += deletedRows;
      log.info("Total {} history removed {} rows", tableName, totalDeletedRowsRows);
      cleanUpFutures.clear();
    } while (deletedRows > 0);

    log.info(
        "---CleanUp{}---- Remove history record has ended. Time taken {}ms",
        tableName,
        System.currentTimeMillis() - currentTime);
  }

  private void refreshMaterializedView(Runnable refreshViewRunnable, String matViewName) {
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
