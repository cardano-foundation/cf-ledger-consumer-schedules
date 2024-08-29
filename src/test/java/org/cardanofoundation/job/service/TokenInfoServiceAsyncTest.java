package org.cardanofoundation.job.service;

import java.util.*;

import org.springframework.boot.test.mock.mockito.MockBean;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.job.repository.ledgersync.MultiAssetRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;

@ExtendWith(MockitoExtension.class)
class TokenInfoServiceAsyncTest {
  @Spy private TokenInfoServiceAsync selfProxyService;
  @Mock private AddressTxAmountRepository addressTxAmountRepository;
  @Mock private MultiAssetRepository multiAssetRepository;
  @Mock private MultiAssetService multiAssetService;
  @InjectMocks private TokenInfoServiceAsync tokenInfoServiceAsync;
  @MockBean private CardanoConverters cardanoConverters;

  //  @Test
  //  @Disabled
  //  void testBuildTokenInfoList() {
  //    List<String> unit = List.of("1L", "2L", "3L");
  //    Long fromSlot = 500L;
  //    Long toSlot = 600L;
  //    Long currentSlot = 1000L;
  //    Timestamp updateTime = Timestamp.valueOf(LocalDateTime.of(2020, 1, 1, 0, 0, 0, 0));
  //    TokenVolume volume24h1 = new TokenVolume("1L", BigInteger.valueOf(100L));
  //    TokenVolume volume24h2 = new TokenVolume("2L", BigInteger.valueOf(200L));
  //    TokenVolume volume24h3 = new TokenVolume("3L", BigInteger.valueOf(300L));
  //    List<TokenVolume> volume24hLst = new ArrayList<>();
  //    volume24hLst.add(volume24h1);
  //    volume24hLst.add(volume24h2);
  //    volume24hLst.add(volume24h3);
  //
  //    TokenVolume totalVolume1 = new TokenVolume("1L", BigInteger.valueOf(10022L));
  //    TokenVolume totalVolume2 = new TokenVolume("2L", BigInteger.valueOf(20022L));
  //    TokenVolume totalVolume3 = new TokenVolume("3L", BigInteger.valueOf(30022L));
  //    List<TokenVolume> tokenVolumes = new ArrayList<>();
  //    tokenVolumes.add(totalVolume1);
  //    tokenVolumes.add(totalVolume2);
  //    tokenVolumes.add(totalVolume3);
  //
  //    TokenTxCount tokenTxCount1 = new TokenTxCount("1L", 100L);
  //    TokenTxCount tokenTxCount2 = new TokenTxCount("2L", 200L);
  //    TokenTxCount tokenTxCount3 = new TokenTxCount("3L", 300L);
  //    List<TokenTxCount> tokenTxCounts = List.of(tokenTxCount1, tokenTxCount2, tokenTxCount3);
  //
  //    Long startIdent = 1L;
  //    Long endIdent = 3L;
  //
  //    when(multiAssetRepository.getTokenUnitByIdBetween(startIdent, endIdent))
  //        .thenReturn(
  //            List.of(
  //                new TokenUnitProjectionImpl(1L, "1L"),
  //                new TokenUnitProjectionImpl(2L, "2L"),
  //                new TokenUnitProjectionImpl(3L, "3L")));
  //    when(addressTxAmountRepository.sumBalanceAfterSlot(anyList(), anyLong()))
  //        .thenReturn(volume24hLst);
  //
  //
  // when(addressTxAmountRepository.getTotalTxCountByUnitIn(anyList())).thenReturn(tokenTxCounts);
  //
  //
  // when(addressTxAmountRepository.getTotalTxCountByUnitIn(anyList())).thenReturn(tokenTxCounts);
  //
  //    when(multiAssetService.getMapNumberHolderByUnits(anyList()))
  //        .thenReturn(
  //            Map.ofEntries(Map.entry("1L", 10L), Map.entry("2L", 20L), Map.entry("3L", 30L)));
  //
  //    List<TokenInfo> result =
  //        tokenInfoServiceAsync.buildTokenInfoList(unit, fromSlot, toSlot, currentSlot);
  //
  //    assertThat(result)
  //        .hasSize(3)
  //        .extracting(
  //            TokenInfo::getVolume24h,
  //            TokenInfo::getTotalVolume,
  //            TokenInfo::getTxCount,
  //            TokenInfo::getNumberOfHolders)
  //        .containsExactlyInAnyOrder(
  //            tuple(
  //                volume24h1.getVolume(),
  //                totalVolume1.getVolume(),
  //                tokenTxCount1.getTxCount(),
  //                10L,
  //                updateTime),
  //            tuple(
  //                volume24h2.getVolume(),
  //                totalVolume2.getVolume(),
  //                tokenTxCount2.getTxCount(),
  //                20L,
  //                updateTime),
  //            tuple(
  //                volume24h3.getVolume(),
  //                totalVolume3.getVolume(),
  //                tokenTxCount3.getTxCount(),
  //                30L,
  //                updateTime));
  //  }
}
