package org.cardanofoundation.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.explorer.common.entity.explorer.TokenInfo;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.repository.ledgersync.AddressTxAmountRepository;

@ExtendWith(MockitoExtension.class)
class TokenInfoServiceAsyncTest {

  @Mock private AddressTxAmountRepository addressTxAmountRepository;
  @Mock private MultiAssetService multiAssetService;
  @InjectMocks private TokenInfoServiceAsync tokenInfoServiceAsync;

  @Test
  void testBuildTokenInfoList() {
    Long blockNo = 1000L;
    Long afterTxId = 100000L;
    Timestamp updateTime = Timestamp.valueOf(LocalDateTime.of(2020, 1, 1, 0, 0, 0, 0));
    TokenVolume volume24h1 = new TokenVolume(1L, BigInteger.valueOf(100L));
    TokenVolume volume24h2 = new TokenVolume(2L, BigInteger.valueOf(200L));
    TokenVolume volume24h3 = new TokenVolume(3L, BigInteger.valueOf(300L));
    List<TokenVolume> volume24hLst = new ArrayList<>();
    volume24hLst.add(volume24h1);
    volume24hLst.add(volume24h2);
    volume24hLst.add(volume24h3);

    TokenVolume totalVolume1 = new TokenVolume(1L, BigInteger.valueOf(10022L));
    TokenVolume totalVolume2 = new TokenVolume(2L, BigInteger.valueOf(20022L));
    TokenVolume totalVolume3 = new TokenVolume(3L, BigInteger.valueOf(30022L));
    List<TokenVolume> tokenVolumes = new ArrayList<>();
    tokenVolumes.add(totalVolume1);
    tokenVolumes.add(totalVolume2);
    tokenVolumes.add(totalVolume3);

    Long startIdent = 1L;
    Long endIdent = 3L;
    when(addressTxAmountRepository.sumBalanceAfterTx(anyLong(), anyLong(), anyLong()))
        .thenReturn(volume24hLst);

    when(addressTxAmountRepository.getTotalVolumeByIdentInRange(anyLong(), anyLong()))
        .thenReturn(tokenVolumes);

    when(multiAssetService.getMapNumberHolder(anyLong(), anyLong()))
        .thenReturn(Map.ofEntries(Map.entry(1L, 10L), Map.entry(2L, 20L), Map.entry(3L, 30L)));

    CompletableFuture<List<TokenInfo>> result =
        tokenInfoServiceAsync.buildTokenInfoList(
            startIdent, endIdent, blockNo, afterTxId, updateTime);
    var tokenInfoListReturned = result.join();

    assertThat(tokenInfoListReturned)
        .hasSize(3)
        .extracting(
            TokenInfo::getBlockNo,
            TokenInfo::getVolume24h,
            TokenInfo::getTotalVolume,
            TokenInfo::getNumberOfHolders,
            TokenInfo::getUpdateTime)
        .containsExactlyInAnyOrder(
            tuple(blockNo, volume24h1.getVolume(), totalVolume1.getVolume(), 10L, updateTime),
            tuple(blockNo, volume24h2.getVolume(), totalVolume2.getVolume(), 20L, updateTime),
            tuple(blockNo, volume24h3.getVolume(), totalVolume3.getVolume(), 30L, updateTime));
  }
}
