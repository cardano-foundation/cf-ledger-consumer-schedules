package org.cardanofoundation.job.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.cardanofoundation.job.model.TokenNumberHolders;
import org.cardanofoundation.job.repository.jooq.JOOQAddressTokenBalanceRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class MultiAssetServiceImplTest {

  @Mock
  private JOOQAddressTokenBalanceRepository jooqAddressTokenBalanceRepository;

  @InjectMocks
  private MultiAssetServiceImpl multiAssetService;

  @Test
  void testGetMapNumberHolder_1() {
    List<Long> multiAssetIds = Arrays.asList(6L, 7L);
    Mockito.when(jooqAddressTokenBalanceRepository.countByMultiAssetIn(multiAssetIds))
        .thenReturn(Arrays.asList(
            new TokenNumberHolders(6L, 20L),
            new TokenNumberHolders(7L, 25L)
        ));
    Mockito.when(
            jooqAddressTokenBalanceRepository.countAddressNotHaveStakeByMultiAssetIn(multiAssetIds))
        .thenReturn(Arrays.asList(
            new TokenNumberHolders(6L, 5L),
            new TokenNumberHolders(7L, 10L)
        ));

    Map<Long, Long> result = multiAssetService.getMapNumberHolder(multiAssetIds);

    assertEquals(20L + 5L, result.get(6L).longValue());
    assertEquals(25L + 10L, result.get(7L).longValue());
  }

  @Test
  void testGetMapNumberHolder_2() {
    List<Long> multiAssetIds = Arrays.asList(6L, 7L);
    Mockito.when(jooqAddressTokenBalanceRepository.countByMultiAssetIn(multiAssetIds))
        .thenReturn(Arrays.asList(
            new TokenNumberHolders(6L, 20L),
            new TokenNumberHolders(7L, 25L)
        ));
    Mockito.when(
            jooqAddressTokenBalanceRepository.countAddressNotHaveStakeByMultiAssetIn(multiAssetIds))
        .thenReturn(Arrays.asList(
            new TokenNumberHolders(6L, 5L)
        ));

    Map<Long, Long> result = multiAssetService.getMapNumberHolder(multiAssetIds);

    assertEquals(20L + 5L, result.get(6L).longValue());
    assertEquals(25L, result.get(7L).longValue());
  }

  @Test
  void testGetMapNumberHolder_3() {
    List<Long> multiAssetIds = Arrays.asList(6L, 7L);
    Mockito.when(jooqAddressTokenBalanceRepository.countByMultiAssetIn(multiAssetIds))
        .thenReturn(Arrays.asList(
            new TokenNumberHolders(6L, 20L)
        ));
    Mockito.when(
            jooqAddressTokenBalanceRepository.countAddressNotHaveStakeByMultiAssetIn(multiAssetIds))
        .thenReturn(Arrays.asList(
            new TokenNumberHolders(6L, 5L),
            new TokenNumberHolders(7L, 10L)
        ));

    Map<Long, Long> result = multiAssetService.getMapNumberHolder(multiAssetIds);

    assertEquals(20L + 5L, result.get(6L).longValue());
    assertEquals(10L, result.get(7L).longValue());
  }
}
