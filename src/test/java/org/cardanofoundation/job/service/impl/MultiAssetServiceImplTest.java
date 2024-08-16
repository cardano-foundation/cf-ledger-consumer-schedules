package org.cardanofoundation.job.service.impl;

import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class MultiAssetServiceImplTest {

  //  @Mock private LatestTokenBalanceRepository latestTokenBalanceRepository;
  //
  //  @InjectMocks private MultiAssetServiceImpl multiAssetService;
  //
  //  @Test
  //  void testGetMapNumberHolder() {
  //    List<String> multiAssetUnitIds = Arrays.asList("unit6", "unit7");
  //
  // Mockito.when(latestTokenBalanceRepository.countHoldersByMultiAssetIdInRange(multiAssetUnitIds))
  //        .thenReturn(
  //            Arrays.asList(
  //                new TokenNumberHolders("unit6", 20L), new TokenNumberHolders("unit7", 25L)));
  //
  //    Map<String, Long> result = multiAssetService.getMapNumberHolderByUnits(multiAssetUnitIds);
  //    assertEquals(20, result.get("unit6").longValue());
  //    assertEquals(25, result.get("unit7").longValue());
  //  }
}
