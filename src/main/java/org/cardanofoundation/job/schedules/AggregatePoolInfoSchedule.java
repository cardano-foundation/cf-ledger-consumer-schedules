package org.cardanofoundation.job.schedules;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.consumercommon.entity.PoolHash;
import org.cardanofoundation.explorer.consumercommon.entity.aggregation.AggregatePoolInfo;
import org.cardanofoundation.job.projection.PoolCountProjection;
import org.cardanofoundation.job.repository.BlockRepository;
import org.cardanofoundation.job.repository.PoolHashRepository;
import org.cardanofoundation.job.repository.aggregate.AggregatePoolInfoRepository;
import org.cardanofoundation.job.service.DelegationService;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class AggregatePoolInfoSchedule {
  final DelegationService delegatorService;

  final BlockRepository blockRepository;
  final AggregatePoolInfoRepository aggregatePoolInfoRepository;
  final PoolHashRepository poolHashRepository;

  @Scheduled(fixedDelayString = "10000")
  @Transactional
  public void updatePoolInfoCache() {
    long startTime = System.currentTimeMillis();
    Map<Long, PoolHash> poolHashMap = poolHashRepository.findAll()
        .parallelStream()
        .collect(Collectors.toMap(PoolHash::getId, Function.identity()));

    Map<Long, AggregatePoolInfo> aggregatePoolInfoMap = aggregatePoolInfoRepository.findAllByPoolIdIn(poolHashMap.keySet())
        .parallelStream()
        .collect(Collectors.toMap(AggregatePoolInfo::getPoolId, Function.identity()));

    aggregatePoolInfoMap.putAll(poolHashMap.keySet().parallelStream()
        .filter(poolId -> !aggregatePoolInfoMap.containsKey(poolId))
        .collect(Collectors.toMap(poolId -> poolId, poolId -> AggregatePoolInfo.builder().poolId(poolId).build())));

    Map<Long, Integer> livePoolDelegatorsCountMap =
        delegatorService.getAllLivePoolDelegatorsCount()
            .parallelStream()
            .collect(Collectors.toMap(PoolCountProjection::getPoolId,
                                      PoolCountProjection::getCountValue));

    Map<Long, Integer> blockLifeTimeMap = blockRepository
        .getCountBlockByPools()
        .parallelStream()
        .collect(
            Collectors.toMap(PoolCountProjection::getPoolId, PoolCountProjection::getCountValue));

    Map<Long, Integer> blockInEpochMap = blockRepository
        .getAllCountBlockInCurrentEpoch()
        .parallelStream()
        .collect(
            Collectors.toMap(PoolCountProjection::getPoolId, PoolCountProjection::getCountValue));

    Timestamp currentTime = Timestamp.valueOf(
        LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));

    aggregatePoolInfoMap
        .entrySet()
        .parallelStream()
        .forEach(entry -> {
          AggregatePoolInfo aggregatePoolInfo = entry.getValue();
          aggregatePoolInfo
              .setDelegatorCount(livePoolDelegatorsCountMap.getOrDefault(entry.getKey(), 0));
          aggregatePoolInfo
              .setBlockLifeTime(blockLifeTimeMap.getOrDefault(entry.getKey(), 0));
          aggregatePoolInfo
              .setBlockInEpoch(blockInEpochMap.getOrDefault(entry.getKey(), 0));
          aggregatePoolInfo.setUpdateTime(currentTime);
          aggregatePoolInfo.setPoolHash(poolHashMap.get(entry.getKey()));
        });

    aggregatePoolInfoRepository.saveAll(aggregatePoolInfoMap.values());
    log.info("Update aggregate pool info done! Time taken: {} ms", System.currentTimeMillis() - startTime);
  }
}
