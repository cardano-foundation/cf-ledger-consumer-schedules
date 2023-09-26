package org.cardanofoundation.job.schedules;

import java.util.Map;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.projection.PoolCountProjection;
import org.cardanofoundation.job.service.DelegationService;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class DelegationSchedule {
  @Value("${application.network}")
  String network;

  final DelegationService delegatorService;

  final RedisTemplate<String, Integer> redisTemplate;

  @Scheduled(fixedRateString = "${jobs.number-delegator.fixed-delay}")
  public void updateNumberDelegator() {
    log.info("Update number of delegator!");
    int numberDelegator = delegatorService.countCurrentDelegator();
    String delegatorKey = getRedisKey(RedisKey.TOTAL_DELEGATOR.name());
    redisTemplate.opsForValue().set(delegatorKey, numberDelegator);
    log.info("Update number of delegator {} successfully", numberDelegator);
  }

  @Scheduled(fixedDelayString = "30000")
  public void updateLivePoolDelegatorsCount(){
    log.info("Update live pool delegators count!");
    long startTime = System.currentTimeMillis();
    Map<Long, Integer> livePoolDelegatorsCountMap =
        delegatorService.getAllLivePoolDelegatorsCount()
            .stream()
            .collect(Collectors.
                         toMap(PoolCountProjection::getPoolId,PoolCountProjection::getCountValue));

    String livePoolDelegatorsCountKey = getRedisKey(RedisKey.POOLS_LIVE_DELEGATORS_COUNT.name());
    redisTemplate.opsForHash().putAll(livePoolDelegatorsCountKey, livePoolDelegatorsCountMap);
    log.info("Update live pool delegators count successfully, Time taken: {} ms",
             System.currentTimeMillis() - startTime);
  }

  private String getRedisKey(String prefix) {
    return prefix + "_" + network;
  }
}
