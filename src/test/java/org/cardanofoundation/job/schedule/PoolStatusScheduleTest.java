package org.cardanofoundation.job.schedule;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.config.RedisTestConfig;
import org.cardanofoundation.job.config.redis.standalone.RedisStandaloneConfig;
import org.cardanofoundation.job.projection.PoolUpdateTxProjection;
import org.cardanofoundation.job.repository.EpochRepository;
import org.cardanofoundation.job.repository.PoolRetireRepository;
import org.cardanofoundation.job.repository.PoolUpdateRepository;
import org.cardanofoundation.job.schedules.PoolStatusSchedule;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ActiveProfiles({"test", "standalone"})
@EnableAutoConfiguration(exclude = RedisAutoConfiguration.class)
@SpringBootTest(classes = {
        RedisStandaloneConfig.class,
        RedisTestConfig.class
})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PoolStatusScheduleTest {

    PoolStatusSchedule poolStatusSchedule;

    @MockBean
    EpochRepository epochRepository;

    @MockBean
    PoolUpdateRepository poolUpdateRepository;

    @MockBean
    PoolRetireRepository poolRetireRepository;

    @Autowired
    RedisTemplate<String, Integer> redisTemplate;


//    @MockBean
//    ValueOperations<String, Integer> valueOperations;

    @BeforeEach
    void init() {
        poolStatusSchedule = new PoolStatusSchedule(epochRepository, poolUpdateRepository, poolRetireRepository, redisTemplate);
        Set<String> keys = redisTemplate.keys("*");
        if (!CollectionUtils.isEmpty(keys)) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void test_getPoolStatus() {
        Mockito.when(poolUpdateRepository.findLastPoolCertificate()).thenReturn(new ArrayList<>(
                List.of(new PoolUpdateTxProjection(100L, 1L),
                        new PoolUpdateTxProjection(200L, 2L),
                        new PoolUpdateTxProjection(300L, 3L)
                )));

        Mockito.when(poolRetireRepository.getLastPoolRetireTilEpoch(Mockito.anyInt())).thenReturn(new ArrayList<>(
                List.of(new PoolUpdateTxProjection(90L, 1L),
                        new PoolUpdateTxProjection(202L, 2L)
                )));
        poolStatusSchedule.updatePoolStatus();
        Mockito.verify(epochRepository, Mockito.times(1)).findMaxEpochNo();
        Mockito.verify(poolUpdateRepository, Mockito.times(1)).findLastPoolCertificate();
        Mockito.verify(poolRetireRepository, Mockito.times(1)).getLastPoolRetireTilEpoch(Mockito.anyInt());
        int poolActivate = redisTemplate.opsForValue().get(RedisKey.POOL_ACTIVATE.name() + "_null");
        int poolInActivate = redisTemplate.opsForValue().get(RedisKey.POOL_INACTIVATE.name() + "_null");
        Assertions.assertEquals(2,poolActivate);
        Assertions.assertEquals(1,poolInActivate);
    }


}
