package org.cardanofoundation.job.schedule;

import static org.mockito.Mockito.when;

import java.util.Set;

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
import org.cardanofoundation.job.provider.RedisProvider;
import org.cardanofoundation.job.schedules.DelegationSchedule;
import org.cardanofoundation.job.service.DelegationService;

@ActiveProfiles({"test", "standalone"})
@EnableAutoConfiguration(exclude = RedisAutoConfiguration.class)
@SpringBootTest(classes = {RedisStandaloneConfig.class, RedisTestConfig.class, RedisProvider.class})
public class DelegationScheduleTest {

  DelegationSchedule delegationSchedule;

  @MockBean DelegationService delegationService;
  @Autowired RedisProvider<String, Integer> redisProvider;
  @Autowired RedisTemplate<String, Integer> redisTemplate;

  @BeforeEach
  void init() {
    delegationSchedule = new DelegationSchedule(delegationService, redisProvider);
    Set<String> keys = redisProvider.keys("*");
    if (!CollectionUtils.isEmpty(keys)) {
      redisProvider.del(keys);
    }
  }

  @Test
  void test_updateNumberDelegator() {
    when(delegationService.countCurrentDelegator()).thenReturn(100);
    delegationSchedule.updateNumberDelegator();
    Mockito.verify(delegationService, Mockito.times(1)).countCurrentDelegator();
    Integer numberDelegator =
        redisTemplate.opsForValue().get(redisProvider.getRedisKey(RedisKey.TOTAL_DELEGATOR.name()));
    Assertions.assertEquals(100, numberDelegator);
  }
}
