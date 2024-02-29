package org.cardanofoundation.job.schedules;

import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.provider.RedisProvider;
import org.cardanofoundation.job.service.PoolService;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PoolStatusSchedule {

  final RedisProvider<String, Integer> redisProvider;

  final PoolService poolService;

  @Scheduled(fixedRateString = "${jobs.pool-status.fixed-delay}")
  public void updatePoolStatus() {
    log.info("Update Pool status!");
    var poolStatus = poolService.getCurrentPoolStatus();
    String poolActivateKey = redisProvider.getRedisKey(RedisKey.POOL_ACTIVATE.name());
    String poolInActivateKey = redisProvider.getRedisKey(RedisKey.POOL_INACTIVATE.name());
    String totalPoolKey = redisProvider.getRedisKey(RedisKey.TOTAL_POOL.name());
    String poolIdsInactivate = redisProvider.getRedisKey(RedisKey.POOL_IDS_INACTIVATE.name());
    int totalPoolSize =
        poolStatus.getPoolActivateIds().size() + poolStatus.getPoolInactivateIds().size();
    redisProvider.setValueByKey(poolActivateKey, poolStatus.getPoolActivateIds().size());
    redisProvider.setValueByKey(poolInActivateKey, poolStatus.getPoolInactivateIds().size());
    redisProvider.setValueByKey(totalPoolKey, totalPoolSize);
    // Delete old pool ids inactivate
    redisProvider.del(poolIdsInactivate);
    redisProvider.putAllHashByKey(
        poolIdsInactivate,
        poolStatus.getPoolInactivateIds().stream()
            .collect(Collectors.toMap(Function.identity(), Function.identity())));
    log.info(
        "Update pool status done! total pool: {}, pool activate {}, pool inactivate {}",
        totalPoolSize,
        poolStatus.getPoolActivateIds().size(),
        poolStatus.getPoolInactivateIds().size());
  }
}
