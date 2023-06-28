package org.cardanofoundation.job.schedule;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.cardanofoundation.job.repository.EpochRepository;
import org.cardanofoundation.job.repository.PoolRetireRepository;
import org.cardanofoundation.job.repository.PoolUpdateRepository;
import org.cardanofoundation.job.schedules.PoolStatusSchedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PoolStatusScheduleTest {

    PoolStatusSchedule poolStatusSchedule;

    @Mock
    EpochRepository epochRepository;

    @Mock
    PoolUpdateRepository poolUpdateRepository;

    @Mock
    PoolRetireRepository poolRetireRepository;

    @Mock
    RedisTemplate<String,Integer> redisTemplate;

    @Mock
    ValueOperations<String, Integer> valueOperations;

    @BeforeEach
    void init(){
        poolStatusSchedule = new PoolStatusSchedule(epochRepository,poolUpdateRepository,poolRetireRepository,redisTemplate);
    }

    @Test
    void Test_getPoolStatus(){
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        poolStatusSchedule.updatePoolStatus();
        Mockito.verify(epochRepository,Mockito.times(1)).findMaxEpochNo();
        Mockito.verify(poolUpdateRepository,Mockito.times(1)).findLastPoolCertificate();
        Mockito.verify(poolRetireRepository,Mockito.times(1)).getLastPoolRetireTilEpoch(Mockito.anyInt());
        Mockito.verify(valueOperations,Mockito.times(2)).set(Mockito.anyString(),Mockito.anyInt());
    }
}
