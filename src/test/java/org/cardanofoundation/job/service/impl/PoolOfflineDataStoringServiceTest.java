package org.cardanofoundation.job.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.explorer.common.entity.ledgersync.PoolHash;
import org.cardanofoundation.explorer.common.entity.ledgersync.PoolMetadataRef;
import org.cardanofoundation.explorer.common.entity.ledgersync.PoolOfflineData;
import org.cardanofoundation.explorer.common.entity.ledgersync.PoolOfflineFetchError;
import org.cardanofoundation.job.dto.PoolData;
import org.cardanofoundation.job.repository.ledgersync.PoolHashRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolMetadataRefRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolOfflineDataRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolOfflineFetchErrorRepository;

@ExtendWith(MockitoExtension.class)
class PoolOfflineDataStoringServiceTest {

  @InjectMocks PoolOfflineDataStoringServiceImpl poolOfflineDataStoringService;
  @Mock PoolMetadataRefRepository poolMetadataRefRepository;
  @Mock PoolOfflineDataRepository poolOfflineDataRepository;
  @Mock PoolHashRepository poolHashRepository;
  @Mock PoolOfflineFetchErrorRepository poolOfflineFetchErrorRepository;
  @Captor ArgumentCaptor<List<PoolOfflineData>> poolOfflineDataCaptor;
  @Captor ArgumentCaptor<Collection<PoolOfflineFetchError>> poolOfflineFetchErrorCaptor;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(poolOfflineDataStoringService, "objectMapper", new ObjectMapper());
  }

  @Test
  @DisplayName("Should process success pool offline data")
  void insertNonExitPoolData() {
    when(poolOfflineDataRepository.findPoolOfflineDataByPoolIdIn(anyList()))
        .thenReturn(Collections.emptyList());

    List<PoolData> successPools = new ArrayList<>();

    String json =
        "{\"name\": \"HOPECHE POOl\",\n"
            + "\"description\": \"Hello This is HOPECHE Staking Pool\",\n"
            + "\"ticker\": \"HCHE\",\n"
            + "\"homepage\": \"https://eyagiz96.github.io/hopechepool\",\n"
            + "\"extended\": \"https://git.io/JnKRd\"\n"
            + "}";

    successPools.add(
        PoolData.builder()
            .poolId(1L)
            .metadataRefId(1L)
            .hash("1")
            .json(json.getBytes())
            .valid(Boolean.TRUE)
            .build());

    PoolHash pool = PoolHash.builder().id(1L).hashRaw("1").epochNo(1).build();

    when(poolHashRepository.findByIdIn(anyList())).thenReturn(List.of(pool));

    when(poolMetadataRefRepository.findByIdIn(anyList()))
        .thenReturn(
            List.of(PoolMetadataRef.builder().id(1L).poolHash(pool).hash("123123").build()));

    poolOfflineDataStoringService.saveSuccessPoolOfflineData(successPools);

    verify(poolOfflineDataRepository, times(1)).saveAll(poolOfflineDataCaptor.capture());

    Assertions.assertEquals(1, poolOfflineDataCaptor.getValue().size());
  }

  @Test
  @DisplayName("Should process success pool offline data with existed pool")
  void insertExistPoolData() {

    PoolHash pool = PoolHash.builder().id(1L).hashRaw("1").epochNo(1).build();

    PoolMetadataRef poolMetadataRef =
        PoolMetadataRef.builder().id(1L).poolHash(pool).hash("123123").build();

    PoolOfflineData poolOfflineData =
        PoolOfflineData.builder()
            .id(1L)
            .pool(pool)
            .poolMetadataRef(poolMetadataRef)
            .poolId(pool.getId())
            .pmrId(poolMetadataRef.getId())
            .hash("2")
            .build();

    when(poolOfflineDataRepository.findPoolOfflineDataByPoolIdIn(anyList()))
        .thenReturn(List.of(poolOfflineData));

    String json =
        "{\"name\": \"HOPECHE POOl\",\n"
            + "\"description\": \"Hello This is HOPECHE Staking Pool\",\n"
            + "\"ticker\": \"HCHE\",\n"
            + "\"homepage\": \"https://eyagiz96.github.io/hopechepool\",\n"
            + "\"extended\": \"https://git.io/JnKRd\"\n"
            + "}";

    List<PoolData> poolDataList = new ArrayList<>();
    poolDataList.add(
        PoolData.builder()
            .poolId(1L)
            .metadataRefId(1L)
            .hash("1")
            .json(json.getBytes())
            .valid(Boolean.TRUE)
            .build());

    when(poolHashRepository.findByIdIn(anyList())).thenReturn(List.of(pool));
    when(poolMetadataRefRepository.findByIdIn(anyList())).thenReturn(List.of(poolMetadataRef));

    poolOfflineDataStoringService.saveSuccessPoolOfflineData(poolDataList);

    verify(poolOfflineDataRepository, times(1)).saveAll(poolOfflineDataCaptor.capture());

    Assertions.assertEquals(1, poolOfflineDataCaptor.getValue().size());

    PoolOfflineData actual = poolOfflineDataCaptor.getValue().get(0);

    Assertions.assertEquals(1L, actual.getId());
    Assertions.assertEquals(1L, actual.getPool().getId());
    Assertions.assertEquals(1L, actual.getPoolMetadataRef().getId());
    Assertions.assertEquals("1", actual.getHash());
  }

  @Test
  @DisplayName("Should process fetch fail pool offline data with existed pool")
  void insertNonExistFailPoolData() {
    PoolHash poolHash = PoolHash.builder().id(1L).hashRaw("1").epochNo(1).build();

    PoolMetadataRef poolMetadataRef =
        PoolMetadataRef.builder().id(1L).poolHash(poolHash).hash("123123").build();

    when(poolHashRepository.findByIdIn(anyList())).thenReturn(List.of(poolHash));
    when(poolMetadataRefRepository.findByIdIn(anyList())).thenReturn(List.of(poolMetadataRef));
    when(poolOfflineFetchErrorRepository.findByPoolHashAndPoolMetadataRef(any(), any()))
        .thenReturn(PoolOfflineFetchError.builder().id(1L).retryCount(1).build());

    poolOfflineDataStoringService.saveFailOfflineData(
        List.of(
            PoolData.builder()
                .poolId(1L)
                .metadataRefId(1L)
                .hash("1")
                .errorMessage("error ")
                .build()));

    verify(poolOfflineFetchErrorRepository, times(1))
        .saveAll(poolOfflineFetchErrorCaptor.capture());

    PoolOfflineFetchError actualError =
        new ArrayList<>(poolOfflineFetchErrorCaptor.getValue()).get(0);
    Assertions.assertEquals(1L, actualError.getId());
    Assertions.assertEquals(2, actualError.getRetryCount());
  }

  @Test
  @DisplayName("Should process fetch fail pool offline data with not existed pool")
  void insertExistFailPoolData() {
    PoolHash poolHash = PoolHash.builder().id(1L).hashRaw("1").epochNo(1).build();

    PoolMetadataRef poolMetadataRef =
        PoolMetadataRef.builder().id(1L).poolHash(poolHash).hash("123123").build();

    when(poolHashRepository.findByIdIn(anyList())).thenReturn(List.of(poolHash));
    when(poolMetadataRefRepository.findByIdIn(anyList())).thenReturn(List.of(poolMetadataRef));
    when(poolOfflineFetchErrorRepository.findByPoolHashAndPoolMetadataRef(any(), any()))
        .thenReturn(null);

    poolOfflineDataStoringService.saveFailOfflineData(
        List.of(
            PoolData.builder()
                .poolId(1L)
                .metadataRefId(1L)
                .hash("1")
                .errorMessage("error ")
                .build()));

    verify(poolOfflineFetchErrorRepository, times(1))
        .saveAll(poolOfflineFetchErrorCaptor.capture());
  }
}
