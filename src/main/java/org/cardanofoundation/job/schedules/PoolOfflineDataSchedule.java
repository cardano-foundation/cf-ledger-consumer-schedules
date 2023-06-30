package org.cardanofoundation.job.schedules;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.job.dto.PoolData;
import org.cardanofoundation.job.event.message.FetchPoolDataFail;
import org.cardanofoundation.job.event.message.FetchPoolDataSuccess;
import org.cardanofoundation.job.service.PoolOfflineDataFetchingService;
import org.cardanofoundation.job.service.PoolOfflineDataStoringService;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConditionalOnProperty(
    value = "jobs.pool-offline-data.enabled",
    matchIfMissing = true,
    havingValue = "true")
public class PoolOfflineDataSchedule {

  final Queue<PoolData> successPools;
  final Queue<PoolData> failPools;
  final PoolOfflineDataStoringService poolOfflineDataStoringService;
  final PoolOfflineDataFetchingService poolOfflineDataFetchingService;
  static final int WAIT_TIMES = 10;

  @Value("${jobs.install-batch}")
  private int batchSize;

  public PoolOfflineDataSchedule(
      PoolOfflineDataStoringService poolOfflineDataStoringService,
      PoolOfflineDataFetchingService poolOfflineDataFetchingService) {
    this.successPools = new LinkedBlockingDeque<>();
    this.failPools = new LinkedBlockingDeque<>();
    this.poolOfflineDataStoringService = poolOfflineDataStoringService;
    this.poolOfflineDataFetchingService = poolOfflineDataFetchingService;
  }

  @Async
  @EventListener
  public void handleSuccessfulPoolData(FetchPoolDataSuccess fetchData) {
    fetchData.getPoolData().setValid(Boolean.TRUE);
    successPools.add(fetchData.getPoolData());
  }

  @Async
  @EventListener
  public void handleFailPoolData(FetchPoolDataFail fetchData) {
    failPools.add(fetchData.getPoolData());
  }

  @Transactional
  @Scheduled(fixedDelayString = "${jobs.pool-offline-data.fetch.delay}")
  public void fetchPoolOffline() throws InterruptedException {
    log.info("Start fetching pool metadata ");
    final var startTime = System.currentTimeMillis();
    final int fetchSize =
        poolOfflineDataFetchingService.fetchPoolOfflineDataByBatch(BigInteger.ZERO.intValue());
    AtomicInteger wait = new AtomicInteger();

    poolOfflineDataFetchingService.fetchPoolOfflineDataLogo(successPools.stream());

    while (successPools.size() + failPools.size() < fetchSize
        && wait.getAndIncrement() < WAIT_TIMES) {
      Thread.sleep(3000);
    }

    log.info("Success pool size {}", successPools.size());
    poolOfflineDataStoringService.insertSuccessPoolOfflineData(successPools);
    successPools.clear();

    log.info("Fail pool size {}", failPools.size());
    poolOfflineDataStoringService.insertFailOfflineData(
        failPools.stream()
            .sorted(
                Comparator.comparing(PoolData::getPoolId).thenComparing(PoolData::getMetadataRefId))
            .toList());
    failPools.clear();
    log.info(
        "-----------------End fetching pool metadata in {} ms",
        (System.currentTimeMillis() - startTime));
  }
}
