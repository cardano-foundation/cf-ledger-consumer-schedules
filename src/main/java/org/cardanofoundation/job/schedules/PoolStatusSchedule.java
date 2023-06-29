package org.cardanofoundation.job.schedules;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.projection.PoolUpdateTxProjection;
import org.cardanofoundation.job.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PoolStatusSchedule {

    final EpochRepository epochRepository;

    final PoolUpdateRepository poolUpdateRepository;

    final PoolRetireRepository poolRetireRepository;

    final RedisTemplate<String,Integer> redisTemplate;

    @Value("${application.network}")
    String network;

    @Scheduled(fixedRateString = "${jobs.pool-status.fixed-delay}")
    public void updatePoolStatus() {
        log.info("Update Pool status!");
        int currentEpoch = epochRepository.findMaxEpochNo();
        var poolCertAndTxId = poolUpdateRepository.findLastPoolCertificate();
        var mPoolRetireCertificate = poolRetireRepository.getLastPoolRetireTilEpoch(currentEpoch).stream()
                .collect(Collectors.toMap(PoolUpdateTxProjection::getPoolId, PoolUpdateTxProjection::getTxId));
        AtomicInteger poolActivateCount = new AtomicInteger(0);
        poolCertAndTxId.forEach(poolUpdateTxProjection -> {
            if(!mPoolRetireCertificate.containsKey(poolUpdateTxProjection.getPoolId())
            || poolUpdateTxProjection.getTxId() > mPoolRetireCertificate.get(poolUpdateTxProjection.getPoolId())){
                poolActivateCount.getAndIncrement();
            }
        });
        String poolActivateKey = getRedisKey(RedisKey.POOL_ACTIVATE.name());
        String poolInActivateKey = getRedisKey(RedisKey.POOL_INACTIVATE.name());
        redisTemplate.opsForValue().set(poolActivateKey,poolActivateCount.get());
        redisTemplate.opsForValue().set(poolInActivateKey,poolCertAndTxId.size() - poolActivateCount.get());
        log.info("Update pool status done! total pool: {}, pool activate {}, pool inactivate {}",poolCertAndTxId.size(), poolActivateCount.get(),
                poolCertAndTxId.size() - poolActivateCount.get());
    }


    private String getRedisKey(String prefix){
        return prefix + "_" + network;
    }
}
