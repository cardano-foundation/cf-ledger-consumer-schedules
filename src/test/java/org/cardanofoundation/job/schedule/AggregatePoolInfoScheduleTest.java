package org.cardanofoundation.job.schedule;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.explorer.consumercommon.entity.PoolHash;
import org.cardanofoundation.explorer.consumercommon.explorer.entity.AggregatePoolInfo;
import org.cardanofoundation.job.projection.PoolCountProjectionImpl;
import org.cardanofoundation.job.repository.explorer.AggregatePoolInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolHashRepository;
import org.cardanofoundation.job.schedules.AggregatePoolInfoSchedule;
import org.cardanofoundation.job.service.DelegationService;

@ExtendWith(MockitoExtension.class)
class AggregatePoolInfoScheduleTest {

  @Mock DelegationService delegationService;
  @Mock BlockRepository blockRepository;
  @Mock AggregatePoolInfoRepository aggregatePoolInfoRepository;
  @Mock PoolHashRepository poolHashRepository;

  @Captor ArgumentCaptor<List<AggregatePoolInfo>> aggregatePoolInfoCaptor;
  AggregatePoolInfoSchedule aggregatePoolInfoSchedule;

  @BeforeEach
  void setUp() {
    aggregatePoolInfoSchedule =
        new AggregatePoolInfoSchedule(
            delegationService, blockRepository, aggregatePoolInfoRepository, poolHashRepository);
  }

  @Test
  void updateAggregatePoolInfoJobTest() {
    PoolHash poolHash1 =
        PoolHash.builder()
            .id(1L)
            .view("pool1q80jjs53w0fx836n8g38gtdwr8ck5zre3da90peuxn84sj3cu0r")
            .build();

    PoolHash poolHash2 =
        PoolHash.builder()
            .id(2L)
            .view("pool1ddskftmsscw92d7vnj89pldwx5feegkgcmamgt5t0e4lkd7mdp8")
            .build();

    PoolCountProjectionImpl pc1 =
        new PoolCountProjectionImpl(
            1L, "pool1q80jjs53w0fx836n8g38gtdwr8ck5zre3da90peuxn84sj3cu0r", 10);

    PoolCountProjectionImpl pc2 =
        new PoolCountProjectionImpl(
            2L, "pool1ddskftmsscw92d7vnj89pldwx5feegkgcmamgt5t0e4lkd7mdp8", 20);

    AggregatePoolInfo api1 = spy(AggregatePoolInfo.builder().poolId(1L).build());

    when(poolHashRepository.findAll()).thenReturn(List.of(poolHash1, poolHash2));
    when(aggregatePoolInfoRepository.findAllByPoolIdIn(Set.of(1L, 2L))).thenReturn(List.of(api1));

    when(delegationService.getAllLivePoolDelegatorsCount()).thenReturn(List.of(pc1, pc2));
    when(blockRepository.getCountBlockByPools()).thenReturn(List.of(pc1, pc2));
    when((blockRepository.getAllCountBlockInCurrentEpoch())).thenReturn(List.of(pc1, pc2));

    aggregatePoolInfoSchedule.updateAggregatePoolInfoJob();

    verify(aggregatePoolInfoRepository, times(1)).saveAll(aggregatePoolInfoCaptor.capture());
    assertEquals(api1.getBlockInEpoch(), 10);
    assertEquals(api1.getDelegatorCount(), 10);
    assertEquals(api1.getBlockLifeTime(), 10);
  }
}
