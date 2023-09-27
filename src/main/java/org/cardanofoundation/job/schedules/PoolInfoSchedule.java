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
import org.cardanofoundation.job.repository.BlockRepository;
import org.cardanofoundation.job.repository.EpochRepository;
import org.cardanofoundation.job.service.DelegationService;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class PoolInfoSchedule {

  @Value("${application.network}")
  String network;

  final DelegationService delegatorService;

  final RedisTemplate<String, Integer> redisTemplate;
  final EpochRepository epochRepository;
  final BlockRepository blockRepository;


  @Scheduled(fixedDelayString = "30000")
  public void updatePoolInfoCache() {
    updateLivePoolDelegatorsCount();
    updateBLockLifeTimePools();
  }


  private void updateLivePoolDelegatorsCount() {
    log.info("Update live pool delegators count!");
    long startTime = System.currentTimeMillis();
    Map<Long, Integer> livePoolDelegatorsCountMap =
        delegatorService.getAllLivePoolDelegatorsCount()
            .stream()
            .collect(Collectors.
                         toMap(PoolCountProjection::getPoolId, PoolCountProjection::getCountValue));

    String livePoolDelegatorsCountKey = getRedisKey(RedisKey.POOLS_LIVE_DELEGATORS_COUNT.name());
    redisTemplate.opsForHash().putAll(livePoolDelegatorsCountKey, livePoolDelegatorsCountMap);
    log.info("Update live pool delegators count successfully, Time taken: {} ms",
             System.currentTimeMillis() - startTime);
  }

  private void updateBLockLifeTimePools() {
    log.info("Update pool block life time!");
    long startTime = System.currentTimeMillis();
    String epochRedisKey = getRedisKey(RedisKey.POOLS_BLOCK_LIFETIME_EPOCH_SNAPSHOT.name());
    Object epochSnapShot = redisTemplate.opsForValue().get(epochRedisKey);

    Integer currentEpochNo = epochRepository.findMaxEpochNo();

    if (epochSnapShot == null || Integer.parseInt(epochSnapShot.toString()) < currentEpochNo) {
      Map<Long, Integer> blockLifeTimeMap = blockRepository.getCountBlockByPools().stream().collect(
          Collectors.toMap(PoolCountProjection::getPoolId,
                           PoolCountProjection::getCountValue));

      String blocKLifeTimeRedisKey = getRedisKey(RedisKey.POOLS_BLOCK_LIFETIME.name());
      redisTemplate.opsForHash().putAll(blocKLifeTimeRedisKey, blockLifeTimeMap);
      redisTemplate.opsForValue().set(epochRedisKey, currentEpochNo);
      log.info("Update pool block life time successfully, Time taken: {} ms",
               System.currentTimeMillis() - startTime);
    } else {
      log.info("Pool block life time is up to date, Skip update!");
    }

  }

  private String getRedisKey(String prefix) {
    return prefix + "_" + network;
  }
}
