package org.cardanofoundation.job.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.math.BigInteger;
import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.explorer.common.entity.explorer.TokenInfo;
import org.cardanofoundation.job.repository.explorer.TokenInfoCheckpointRepository;
import org.cardanofoundation.job.repository.explorer.TokenInfoRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.service.TokenInfoServiceAsync;

@ExtendWith(MockitoExtension.class)
class TokenInfoServiceImplTest {

  @Mock private TokenInfoCheckpointRepository tokenInfoCheckpointRepository;
  @Mock private TokenInfoRepository tokenInfoRepository;
  @Mock private TokenInfoServiceAsync tokenInfoServiceAsync;
  @Mock private AddressTxAmountRepository addressTxAmountRepository;

  @InjectMocks private TokenInfoServiceImpl tokenInfoService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(tokenInfoService, "network", "mainnet");
    ReflectionTestUtils.setField(tokenInfoService, "NUM_SLOT_INTERVAL", 100);
  }

  @Test
  void testProcessTokenInSlotRange_runWithInitMode() {
    Long fromSlot = 500L;
    Long toSlot = 600L;
    Long currentSlot = 1000L;
    List<String> units = List.of("unit1", "unit2", "unit3", "unit4");
    when(addressTxAmountRepository.getTokensInTransactionInSlotRange(any(), any()))
        .thenReturn(units);
    TokenInfo tokenInfo1 =
        TokenInfo.builder()
            .unit("unit1")
            .updatedSlot(600L)
            .totalVolume(BigInteger.valueOf(100L))
            .numberOfHolders(10L)
            .volume24h(BigInteger.ZERO)
            .build();

    TokenInfo tokenInfo2 =
        TokenInfo.builder()
            .unit("unit2")
            .updatedSlot(600L)
            .totalVolume(BigInteger.valueOf(110L))
            .numberOfHolders(10L)
            .volume24h(BigInteger.ZERO)
            .build();

    TokenInfo tokenInfo3 =
        TokenInfo.builder()
            .unit("unit3")
            .updatedSlot(600L)
            .totalVolume(BigInteger.valueOf(90L))
            .numberOfHolders(10L)
            .volume24h(BigInteger.ZERO)
            .build();

    TokenInfo tokenInfo4 =
        TokenInfo.builder()
            .updatedSlot(600L)
            .totalVolume(BigInteger.valueOf(90L))
            .numberOfHolders(10L)
            .volume24h(BigInteger.ZERO)
            .build();
    when(tokenInfoServiceAsync.buildTokenInfoList(anyList(), any(), any(), any()))
        .thenReturn(List.of(tokenInfo1, tokenInfo2, tokenInfo3, tokenInfo4));

    TokenInfo tokenInfoDb1 =
        TokenInfo.builder()
            .unit("unit1")
            .totalVolume(BigInteger.valueOf(12))
            .numberOfHolders(10L)
            .updatedSlot(800L)
            .isCalculatedInIncrementalMode(true)
            .build();

    TokenInfo tokenInfoDb2 =
        TokenInfo.builder()
            .unit("unit2")
            .totalVolume(BigInteger.valueOf(12))
            .numberOfHolders(5L)
            .updatedSlot(400L)
            .volume24h(BigInteger.valueOf(10))
            .previousSlot(300L)
            .previousTotalVolume(BigInteger.TEN)
            .previousNumberOfHolders(5L)
            .isCalculatedInIncrementalMode(false)
            .build();

    TokenInfo tokenInfoDb3 =
        TokenInfo.builder()
            .unit("unit3")
            .totalVolume(BigInteger.valueOf(12))
            .numberOfHolders(5L)
            .updatedSlot(400L)
            .volume24h(BigInteger.valueOf(10))
            .previousSlot(300L)
            .previousTotalVolume(BigInteger.TEN)
            .previousNumberOfHolders(5L)
            .isCalculatedInIncrementalMode(true)
            .build();

    when(tokenInfoRepository.findByUnitIn(anyList()))
        .thenReturn(List.of(tokenInfoDb1, tokenInfoDb2, tokenInfoDb3));

    var actual = tokenInfoService.processTokenInSlotRange(fromSlot, toSlot, currentSlot);

    Assertions.assertNotNull(actual);
    Assertions.assertEquals(600L, actual.getSlot());
  }
}
