package org.cardanofoundation.job.schedule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.CollectionUtils;

import org.mockito.Mockito;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.config.RedisTestConfig;
import org.cardanofoundation.job.config.redis.standalone.RedisStandaloneConfig;
import org.cardanofoundation.job.dto.PoolStatus;
import org.cardanofoundation.job.provider.RedisProvider;
import org.cardanofoundation.job.schedules.PoolStatusSchedule;
import org.cardanofoundation.job.service.PoolService;

@ActiveProfiles({"test", "standalone"})
@EnableAutoConfiguration(exclude = RedisAutoConfiguration.class)
@SpringBootTest(classes = {RedisStandaloneConfig.class, RedisTestConfig.class, RedisProvider.class})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PoolStatusScheduleTest {

  PoolStatusSchedule poolStatusSchedule;

  @MockBean PoolService poolService;

  @Autowired RedisProvider<String, Integer> redisProvider;
  @Autowired RedisTemplate<String, Integer> redisTemplate;

  @BeforeEach
  void init() {
    poolStatusSchedule = new PoolStatusSchedule(redisProvider, poolService);
    Set<String> keys = redisProvider.keys("*");
    if (!CollectionUtils.isEmpty(keys)) {
      redisProvider.del(keys);
    }
  }

  @Test
  void test_getPoolStatus() {
    Mockito.when(poolService.getCurrentPoolStatus())
        .thenReturn(
            PoolStatus.builder()
                .poolActivateIds(new HashSet<>(List.of(1L, 2L)))
                .poolInactivateIds(new HashSet<>(List.of(3L)))
                .build());
    poolStatusSchedule.updatePoolStatus();
    Mockito.verify(poolService, Mockito.times(1)).getCurrentPoolStatus();
    int poolActivate =
        redisTemplate.opsForValue().get(redisProvider.getRedisKey(RedisKey.POOL_ACTIVATE.name()));
    int poolInActivate =
        redisTemplate.opsForValue().get(redisProvider.getRedisKey(RedisKey.POOL_INACTIVATE.name()));
    Assertions.assertEquals(2, poolActivate);
    Assertions.assertEquals(1, poolInActivate);
  }
}
