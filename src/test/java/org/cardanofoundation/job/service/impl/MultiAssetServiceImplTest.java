package org.cardanofoundation.job.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.job.model.TokenNumberHolders;
import org.cardanofoundation.job.repository.ledgersync.LatestTokenBalanceRepository;

@ExtendWith(MockitoExtension.class)
class MultiAssetServiceImplTest {

  @Mock private LatestTokenBalanceRepository latestTokenBalanceRepository;

  @InjectMocks private MultiAssetServiceImpl multiAssetService;

  @Test
  void testGetMapNumberHolder_1() {
    List<Long> multiAssetIds = Arrays.asList(6L, 7L);
    Mockito.when(latestTokenBalanceRepository.countHoldersByMultiAssetIdIn(multiAssetIds))
        .thenReturn(
            Arrays.asList(new TokenNumberHolders(6L, 20L), new TokenNumberHolders(7L, 25L)));

    Map<Long, Long> result = multiAssetService.getMapNumberHolder(multiAssetIds);
    assertEquals(20, result.get(6L).longValue());
    assertEquals(25, result.get(7L).longValue());
  }

  @Test
  void testGetMapNumberHolder_2() {
    Long startIdent = 6L;
    Long endIdent = 7L;
    Mockito.when(
            latestTokenBalanceRepository.countHoldersByMultiAssetIdInRange(startIdent, endIdent))
        .thenReturn(
            Arrays.asList(new TokenNumberHolders(6L, 20L), new TokenNumberHolders(7L, 25L)));

    Map<Long, Long> result = multiAssetService.getMapNumberHolder(startIdent, endIdent);
    assertEquals(20, result.get(6L).longValue());
    assertEquals(25, result.get(7L).longValue());
  }
}
