package org.cardanofoundation.job.service.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.cardanofoundation.job.config.RedisTestConfig;
import org.cardanofoundation.job.config.redis.standalone.RedisStandaloneConfig;
import org.cardanofoundation.job.projection.PoolUpdateTxProjection;
import org.cardanofoundation.job.repository.EpochRepository;
import org.cardanofoundation.job.repository.PoolRetireRepository;
import org.cardanofoundation.job.repository.PoolUpdateRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@ActiveProfiles({"test", "standalone"})
@EnableAutoConfiguration(exclude = RedisAutoConfiguration.class)
@SpringBootTest(classes = {
       PoolServiceImpl.class
})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PoolServiceTest {

    PoolServiceImpl poolService;
    @MockBean
    EpochRepository epochRepository;

    @MockBean
    PoolUpdateRepository poolUpdateRepository;

    @MockBean
    PoolRetireRepository poolRetireRepository;

    @BeforeEach
    void init(){
        poolService = new PoolServiceImpl(epochRepository,poolUpdateRepository,poolRetireRepository);
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
        var poolStatus = poolService.getCurrentPoolStatus();
        Mockito.verify(epochRepository, Mockito.times(1)).findMaxEpochNo();
        Mockito.verify(poolUpdateRepository, Mockito.times(1)).findLastPoolCertificate();
        Mockito.verify(poolRetireRepository, Mockito.times(1)).getLastPoolRetireTilEpoch(Mockito.anyInt());
        Assertions.assertTrue(poolStatus.getPoolActivateIds().containsAll(new HashSet<>(List.of(1L,3L))));
        Assertions.assertTrue(poolStatus.getPoolInactivateIds().containsAll(new HashSet<>(List.of(2L))));
    }
}
