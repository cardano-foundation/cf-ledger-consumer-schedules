package org.cardanofoundation.job.service.impl;

import java.util.HashSet;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import org.mockito.Mockito;

import org.junit.jupiter.api.Test;

import org.cardanofoundation.job.dto.PoolStatus;
import org.cardanofoundation.job.repository.DelegationRepository;
import org.cardanofoundation.job.service.PoolService;

@ActiveProfiles({"test", "standalone"})
@EnableAutoConfiguration(exclude = RedisAutoConfiguration.class)
@SpringBootTest(classes = {DelegationServiceImpl.class})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DelegationServiceTest {

  @MockBean PoolService poolService;

  @MockBean DelegationRepository delegationRepository;

  @Autowired DelegationServiceImpl delegationService;

  @Test
  void test_countCurrentDelegator() {
    Mockito.when(poolService.getCurrentPoolStatus())
        .thenReturn(new PoolStatus(new HashSet<>(), new HashSet<>()));
    delegationService.countCurrentDelegator();
    Mockito.verify(poolService, Mockito.times(1)).getCurrentPoolStatus();
    Mockito.verify(delegationRepository, Mockito.times(1)).countCurrentDelegator(Mockito.anySet());
  }
}
