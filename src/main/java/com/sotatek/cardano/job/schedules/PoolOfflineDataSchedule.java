package com.sotatek.cardano.job.schedules;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.sotatek.cardano.job.dto.PoolData;
import com.sotatek.cardano.job.event.message.FetchPoolDataSuccess;
import com.sotatek.cardano.job.service.interfaces.PoolOfflineDataFetchingService;
import com.sotatek.cardano.job.service.interfaces.PoolOfflineDataStoringService;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PoolOfflineDataSchedule {

  final Queue<PoolData> successPools;
  final PoolOfflineDataStoringService poolOfflineDataStoringService;
  final PoolOfflineDataFetchingService poolOfflineDataFetchingService;

  @Value("${jobs.install-batch}")
  private int batchSize;

  public PoolOfflineDataSchedule(
      PoolOfflineDataStoringService poolOfflineDataStoringService,
      PoolOfflineDataFetchingService poolOfflineDataFetchingService) {
    this.successPools = new LinkedBlockingDeque<>();
    this.poolOfflineDataStoringService = poolOfflineDataStoringService;
    this.poolOfflineDataFetchingService = poolOfflineDataFetchingService;
  }

  @Async
  @EventListener
  public void handleSuccessfulPoolData(FetchPoolDataSuccess fetchData) {
    successPools.add(fetchData.getPoolData());
  }

  @Transactional
  @Scheduled(
      fixedDelayString = "${jobs.insert-pool-offline-data.delay}",
      initialDelayString = "${jobs.insert-pool-offline-data.innit}")
  public void updatePoolOffline() throws InterruptedException {
    log.info("pool size {}", successPools.size());

    if (CollectionUtils.isEmpty(successPools)) {
      Thread.sleep(3000);
    }

    Set<PoolData> poolData = new HashSet<>();

    while (successPools.size() > BigInteger.ZERO.intValue()) {
      poolData.add(successPools.poll());
      if (poolData.size() == batchSize) {
        poolOfflineDataStoringService.insertBatch(poolData);
        poolData.clear();
      }
    }

    if (!CollectionUtils.isEmpty(poolData)) {
      poolOfflineDataStoringService.insertBatch(poolData);
      poolData.clear();
    }
  }

  @Scheduled(fixedDelayString = "${jobs.fetch-pool-offline-data.delay}")
  public void fetchPoolOffline() {
    poolOfflineDataFetchingService.fetchBatch(BigInteger.ZERO.intValue());
  }
}
