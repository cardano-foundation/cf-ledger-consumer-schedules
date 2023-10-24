package org.cardanofoundation.job.service;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.cardanofoundation.explorer.consumercommon.entity.MultiAsset;
import org.cardanofoundation.explorer.consumercommon.explorer.entity.TokenInfo;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.repository.ledgersync.jooq.JOOQAddressTokenRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class TokenInfoServiceAsyncTest {

  @Mock
  private JOOQAddressTokenRepository jooqAddressTokenRepository;
  @Mock
  private MultiAssetService multiAssetService;
  @InjectMocks
  private TokenInfoServiceAsync tokenInfoServiceAsync;

  @Test
  void testBuildTokenInfoList() {
    MultiAsset multiAsset1 = Mockito.mock(MultiAsset.class);
    when(multiAsset1.getId()).thenReturn(1L);
    MultiAsset multiAsset2 = Mockito.mock(MultiAsset.class);
    when(multiAsset2.getId()).thenReturn(2L);
    MultiAsset multiAsset3 = Mockito.mock(MultiAsset.class);
    when(multiAsset3.getId()).thenReturn(3L);
    List<MultiAsset> multiAssetList = Arrays.asList(multiAsset1, multiAsset2, multiAsset3);
    Long blockNo = 1000L;
    Long afterTxId = 100000L;
    Timestamp updateTime = Timestamp.valueOf(LocalDateTime.of(2020, 1, 1, 0, 0, 0, 0));
    TokenVolume tokenVolume1 = new TokenVolume(1L, BigInteger.valueOf(100L));
    TokenVolume tokenVolume2 = new TokenVolume(2L, BigInteger.valueOf(200L));
    TokenVolume tokenVolume3 = new TokenVolume(3L, BigInteger.valueOf(300L));
    List<TokenVolume> tokenVolumes = new ArrayList<>();
    tokenVolumes.add(tokenVolume1);
    tokenVolumes.add(tokenVolume2);
    tokenVolumes.add(tokenVolume3);
    when(jooqAddressTokenRepository.sumBalanceAfterTx(anyList(), anyLong()))
        .thenReturn(tokenVolumes);

    when(multiAssetService.getMapNumberHolder(anyList()))
        .thenReturn(
            Map.ofEntries(
                Map.entry(1L, 10L),
                Map.entry(2L, 20L),
                Map.entry(3L, 30L)
            )
        );

    CompletableFuture<List<TokenInfo>> result = tokenInfoServiceAsync.buildTokenInfoList(
        multiAssetList, blockNo, afterTxId, updateTime);
    var tokenInfoListReturned = result.join();

    assertThat(tokenInfoListReturned).hasSize(3)
        .extracting(
            TokenInfo::getBlockNo,
            TokenInfo::getVolume24h,
            TokenInfo::getNumberOfHolders,
            TokenInfo::getUpdateTime)
        .containsExactlyInAnyOrder(
            tuple(blockNo, tokenVolume1.getVolume(),
                10L, updateTime),
            tuple(blockNo, tokenVolume2.getVolume(),
                20L, updateTime),
            tuple(blockNo, tokenVolume3.getVolume(),
                30L, updateTime)
        );
  }
}
