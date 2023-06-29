package org.cardanofoundation.job.schedule;

import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.config.RedisTestConfig;
import org.cardanofoundation.job.config.redis.standalone.RedisStandaloneConfig;
import org.cardanofoundation.job.schedules.DelegationSchedule;
import org.cardanofoundation.job.schedules.PoolStatusSchedule;
import org.cardanofoundation.job.service.DelegationService;
import org.cardanofoundation.job.service.impl.DelegationServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.CollectionUtils;

import java.util.Set;

@ActiveProfiles({"test", "standalone"})
@EnableAutoConfiguration(exclude = RedisAutoConfiguration.class)
@SpringBootTest(classes = {
        RedisStandaloneConfig.class,
        RedisTestConfig.class
})
public class DelegationScheduleTest {

    DelegationSchedule delegationSchedule;

    @MockBean
    DelegationService delegationService;

    @Autowired
    RedisTemplate<String, Integer> redisTemplate;

    @BeforeEach
    void init() {
        delegationSchedule = new DelegationSchedule(delegationService,redisTemplate);
        Set<String> keys = redisTemplate.keys("*");
        if (!CollectionUtils.isEmpty(keys)) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void test_updateNumberDelegator(){
        Mockito.when(delegationService.countCurrentDelegator()).thenReturn(100);
        delegationSchedule.updateNumberDelegator();
        Mockito.verify(delegationService,Mockito.times(1)).countCurrentDelegator();
        int numberDelegator = redisTemplate.opsForValue().get(RedisKey.TOTAL_DELEGATOR.name() + "_null");
        Assertions.assertEquals(100,numberDelegator);
    }
}
