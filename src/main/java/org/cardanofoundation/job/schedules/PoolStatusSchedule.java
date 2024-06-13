package org.cardanofoundation.job.schedules;

import java.util.function.Function;
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
import org.cardanofoundation.job.service.PoolService;

//@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PoolStatusSchedule {

  final RedisTemplate<String, Integer> redisTemplate;

  final PoolService poolService;

  @Value("${application.network}")
  String network;

  @Scheduled(fixedRateString = "${jobs.pool-status.fixed-delay}")
  public void updatePoolStatus() {
    log.info("Update Pool status!");
    var poolStatus = poolService.getCurrentPoolStatus();
    String poolActivateKey = getRedisKey(RedisKey.POOL_ACTIVATE.name());
    String poolInActivateKey = getRedisKey(RedisKey.POOL_INACTIVATE.name());
    String totalPoolKey = getRedisKey(RedisKey.TOTAL_POOL.name());
    String poolIdsInactivate = getRedisKey(RedisKey.POOL_IDS_INACTIVATE.name());
    int totalPoolSize =
        poolStatus.getPoolActivateIds().size() + poolStatus.getPoolInactivateIds().size();
    redisTemplate.opsForValue().set(poolActivateKey, poolStatus.getPoolActivateIds().size());
    redisTemplate.opsForValue().set(poolInActivateKey, poolStatus.getPoolInactivateIds().size());
    redisTemplate.opsForValue().set(totalPoolKey, totalPoolSize);

    // Delete old pool ids inactivate
    redisTemplate.delete(poolIdsInactivate);
    redisTemplate
        .opsForHash()
        .putAll(
            poolIdsInactivate,
            poolStatus.getPoolInactivateIds().stream()
                .collect(Collectors.toMap(Function.identity(), Function.identity())));
    log.info(
        "Update pool status done! total pool: {}, pool activate {}, pool inactivate {}",
        totalPoolSize,
        poolStatus.getPoolActivateIds().size(),
        poolStatus.getPoolInactivateIds().size());
  }

  private String getRedisKey(String prefix) {
    return prefix + "_" + network;
  }
}
