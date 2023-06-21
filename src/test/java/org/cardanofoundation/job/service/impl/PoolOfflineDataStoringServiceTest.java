package org.cardanofoundation.job.service.impl;

import java.util.Collections;

import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cardanofoundation.job.repository.PoolHashRepository;
import org.cardanofoundation.job.repository.PoolMetadataRefRepository;
import org.cardanofoundation.job.repository.PoolOfflineDataRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class PoolOfflineDataStoringServiceTest {

  @InjectMocks
  PoolOfflineDataStoringServiceImpl poolOfflineDataStoringService;
  @Mock
  PoolMetadataRefRepository poolMetadataRefRepository;
  @Mock
  PoolOfflineDataRepository poolOfflineDataRepository;
  @Mock
  PoolHashRepository poolHashRepository;

  @Test
  void insertNonExitPoolData(){

  }

  @Test
  void insertExitPoolData(){

  }

  @Test
  void insertNonExistFailPoolData(){

  }


  @Test
  void insertExistFailPoolData(){

  }

}
