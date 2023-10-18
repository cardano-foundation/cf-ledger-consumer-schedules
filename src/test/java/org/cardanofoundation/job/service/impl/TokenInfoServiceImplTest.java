package org.cardanofoundation.job.service.impl;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.cardanofoundation.explorer.consumercommon.entity.Block;
import org.cardanofoundation.explorer.consumercommon.entity.MultiAsset;
import org.cardanofoundation.explorer.consumercommon.entity.TokenInfo;
import org.cardanofoundation.explorer.consumercommon.entity.TokenInfoCheckpoint;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.MultiAssetRepository;
import org.cardanofoundation.job.repository.ledgersync.TokenInfoCheckpointRepository;
import org.cardanofoundation.job.repository.ledgersync.TokenInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.TxRepository;
import org.cardanofoundation.job.repository.ledgersync.jooq.JOOQAddressTokenRepository;
import org.cardanofoundation.job.repository.ledgersync.jooq.JOOQMultiAssetRepository;
import org.cardanofoundation.job.repository.ledgersync.jooq.JOOQTokenInfoRepository;
import org.cardanofoundation.job.service.MultiAssetService;
import org.cardanofoundation.job.service.TokenInfoServiceAsync;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class TokenInfoServiceImplTest {

  @Mock
  private TokenInfoCheckpointRepository tokenInfoCheckpointRepository;
  @Mock
  private BlockRepository blockRepository;
  @Mock
  private MultiAssetRepository multiAssetRepository;
  @Mock
  private TokenInfoRepository tokenInfoRepository;
  @Mock
  private TokenInfoServiceAsync tokenInfoServiceAsync;
  @Mock
  private TxRepository txRepository;
  @Mock
  private JOOQTokenInfoRepository jooqTokenInfoRepository;
  @Mock
  private JOOQMultiAssetRepository jooqMultiAssetRepository;
  @Mock
  private JOOQAddressTokenRepository jooqAddressTokenRepository;
  @Mock
  private MultiAssetService multiAssetService;
  @Captor
  private ArgumentCaptor<List<TokenInfo>> tokenInfosCaptor;
  @Captor
  private ArgumentCaptor<TokenInfoCheckpoint> tokenInfoCheckpointCaptor;

  @InjectMocks
  private TokenInfoServiceImpl tokenInfoService;

  @Test
  void testUpdateTokenInfoListForFirstTime() {
    Block latestBlock = Mockito.mock(Block.class);
    when(latestBlock.getBlockNo()).thenReturn(10000L);
    when(latestBlock.getTime()).thenReturn(
        Timestamp.valueOf(LocalDateTime.of(2021, 11, 5, 11, 15, 13)));
    when(blockRepository.findLatestBlock()).thenReturn(Optional.of(latestBlock));
    when(tokenInfoCheckpointRepository.findLatestTokenInfoCheckpoint())
        .thenReturn(Optional.empty());
    long multiAssetCount = 180000;
    when(multiAssetRepository.count()).thenReturn(multiAssetCount);

    List<MultiAsset> multiAssets = new ArrayList<>();
    IntStream.range(0, (int) multiAssetCount).forEach(value -> multiAssets.add(new MultiAsset()));

    when(jooqMultiAssetRepository.getMultiAsset(anyInt(), anyInt()))
        .thenAnswer(invocation -> {
          int page = invocation.getArgument(0);
          int subListSize = invocation.getArgument(1);
          int startIndex = page * subListSize;
          int endIndex = Math.min(startIndex + subListSize, multiAssets.size());
          return multiAssets.subList(startIndex, endIndex);
        });

    when(txRepository.findMinTxByAfterTime(any())).thenReturn(Optional.of(200000L));
    when(tokenInfoServiceAsync.buildTokenInfoList(anyList(), anyLong(), anyLong(),
        any(Timestamp.class)))
        .thenAnswer(invocation -> {
          List<MultiAsset> subList = invocation.getArgument(0);
          List<TokenInfo> mockTokenInfoList = new ArrayList<>();
          IntStream.range(0, subList.size()).forEach(i -> mockTokenInfoList.add(new TokenInfo()));
          return CompletableFuture.completedFuture(mockTokenInfoList);
        });

    tokenInfoService.updateTokenInfoList();

    verify(jooqTokenInfoRepository, atLeastOnce()).insertAll(tokenInfosCaptor.capture());
    verify(tokenInfoCheckpointRepository, times(1)).save(
        tokenInfoCheckpointCaptor.capture());
    assertEquals((int) multiAssetCount,
        tokenInfosCaptor.getAllValues().stream().mapToInt(List::size).sum());
    var tokenInfoCheckpointSaved = tokenInfoCheckpointCaptor.getValue();
    assertEquals(latestBlock.getBlockNo(), tokenInfoCheckpointSaved.getBlockNo());
    assertEquals(latestBlock.getTime(), tokenInfoCheckpointSaved.getUpdateTime());
  }

  @Test
  void testUpdateTokenInfoListForFirstTime_WhenGetMultiAssetsFailed_ShouldThrowException() {
    Block latestBlock = Mockito.mock(Block.class);
    when(latestBlock.getBlockNo()).thenReturn(10000L);
    when(latestBlock.getTime()).thenReturn(
        Timestamp.valueOf(LocalDateTime.of(2021, 11, 5, 11, 15, 13)));
    when(blockRepository.findLatestBlock()).thenReturn(Optional.of(latestBlock));
    when(tokenInfoCheckpointRepository.findLatestTokenInfoCheckpoint())
        .thenReturn(Optional.empty());
    long multiAssetCount = 180000;
    when(multiAssetRepository.count()).thenReturn(multiAssetCount);
    when(jooqMultiAssetRepository.getMultiAsset(anyInt(), anyInt())).thenThrow(RuntimeException.class);

    assertThrows(RuntimeException.class, () -> tokenInfoService.updateTokenInfoList());
  }

  @Test
  void testUpdateTokenInfoListForFirstTime_WhenBuildTokenInfoListFailed_ShouldThrowException() {
    Block latestBlock = Mockito.mock(Block.class);
    when(latestBlock.getBlockNo()).thenReturn(10000L);
    when(latestBlock.getTime()).thenReturn(
        Timestamp.valueOf(LocalDateTime.of(2021, 11, 5, 11, 15, 13)));
    when(blockRepository.findLatestBlock()).thenReturn(Optional.of(latestBlock));
    when(tokenInfoCheckpointRepository.findLatestTokenInfoCheckpoint())
        .thenReturn(Optional.empty());
    long multiAssetCount = 180000;
    when(multiAssetRepository.count()).thenReturn(multiAssetCount);

    List<MultiAsset> multiAssets = new ArrayList<>();
    IntStream.range(0, (int) multiAssetCount).forEach(value -> multiAssets.add(new MultiAsset()));

    when(jooqMultiAssetRepository.getMultiAsset(anyInt(), anyInt()))
        .thenAnswer(invocation -> {
          int page = invocation.getArgument(0);
          int subListSize = invocation.getArgument(1);
          int startIndex = page * subListSize;
          int endIndex = Math.min(startIndex + subListSize, multiAssets.size());
          return multiAssets.subList(startIndex, endIndex);
        });

    when(txRepository.findMinTxByAfterTime(any())).thenReturn(Optional.of(200000L));
    when(tokenInfoServiceAsync.buildTokenInfoList(anyList(), anyLong(), anyLong(),
        any(Timestamp.class))).thenThrow(RuntimeException.class);

    assertThrows(RuntimeException.class, () -> tokenInfoService.updateTokenInfoList());
  }

  @Test
  void testUpdateTokenInfoListNonInitialUpdate() {
    Block latestBlock = Mockito.mock(Block.class);
    when(latestBlock.getBlockNo()).thenReturn(9999L);
    when(latestBlock.getTime()).thenReturn(
        Timestamp.valueOf(LocalDateTime.of(2023, 10, 4, 11, 0, 0)));

    when(blockRepository.findLatestBlock()).thenReturn(Optional.of(latestBlock));

    TokenInfoCheckpoint tokenInfoCheckpoint = Mockito.mock(TokenInfoCheckpoint.class);
    when(tokenInfoCheckpoint.getBlockNo()).thenReturn(9999L);
    when(tokenInfoCheckpoint.getUpdateTime()).thenReturn(
        Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC).minusHours(1)));

    when(tokenInfoCheckpointRepository.findLatestTokenInfoCheckpoint())
        .thenReturn(Optional.of(tokenInfoCheckpoint));
    Long txId = 10000L;
    when(txRepository.findMinTxByAfterTime(any())).thenReturn(Optional.of(txId));

    MultiAsset tokensInTransactionWithNewBlockRange = Mockito.mock(MultiAsset.class);
    when(tokensInTransactionWithNewBlockRange.getId()).thenReturn(1L);
    when(multiAssetRepository.getTokensInTransactionInBlockRange(anyLong(), anyLong()))
        .thenReturn(List.of(tokensInTransactionWithNewBlockRange));

    MultiAsset tokensWithZeroTxCount = Mockito.mock(MultiAsset.class);
    when(tokensWithZeroTxCount.getId()).thenReturn(2L);
    when(multiAssetRepository.getTokensWithZeroTxCountAndAfterTime(
        any())).thenReturn(List.of(tokensWithZeroTxCount));

    MultiAsset tokenNeedUpdateVolume24h = Mockito.mock(MultiAsset.class);
    when(tokenNeedUpdateVolume24h.getId()).thenReturn(3L);

    when(multiAssetRepository.getTokensInTransactionInTimeRange(any(), any())).thenReturn(
        List.of(tokenNeedUpdateVolume24h));

    final TokenVolume tokenVolume1 = new TokenVolume(1L, BigInteger.valueOf(100L));
    final TokenVolume tokenVolume2 = new TokenVolume(2L, BigInteger.valueOf(200L));
    final TokenVolume tokenVolume3 = new TokenVolume(3L, BigInteger.valueOf(300L));
    final List<TokenVolume> tokenVolumes = List.of(tokenVolume1, tokenVolume2, tokenVolume3);
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

    TokenInfo tokenInfo1 = spy(TokenInfo.class);
    when(tokenInfo1.getMultiAssetId()).thenReturn(1L);
    TokenInfo tokenInfo2 = spy(TokenInfo.class);
    when(tokenInfo2.getMultiAssetId()).thenReturn(2L);
    TokenInfo tokenInfo3 = spy(TokenInfo.class);
    when(tokenInfo3.getMultiAssetId()).thenReturn(3L);
    when(tokenInfoRepository.findByMultiAssetIdIn(anyCollection())).thenReturn(
        List.of(tokenInfo1, tokenInfo2, tokenInfo3));

    tokenInfoService.updateTokenInfoList();

    verify(tokenInfoRepository).saveAll(tokenInfosCaptor.capture());
    List<TokenInfo> tokenInfosSaved = tokenInfosCaptor.getValue();
    assertThat(tokenInfosSaved).hasSize(3)
        .extracting(
            TokenInfo::getBlockNo,
            TokenInfo::getVolume24h,
            TokenInfo::getNumberOfHolders,
            TokenInfo::getUpdateTime)
        .containsExactlyInAnyOrder(
            tuple(latestBlock.getBlockNo(), tokenVolume1.getVolume(),
                10L, latestBlock.getTime()),
            tuple(latestBlock.getBlockNo(), tokenVolume2.getVolume(),
                20L, latestBlock.getTime()),
            tuple(latestBlock.getBlockNo(), tokenVolume3.getVolume(),
                30L, latestBlock.getTime())
        );

    verify(tokenInfoCheckpointRepository).save(tokenInfoCheckpointCaptor.capture());
    TokenInfoCheckpoint checkpointSaved = tokenInfoCheckpointCaptor.getValue();
    assertEquals(latestBlock.getBlockNo(), checkpointSaved.getBlockNo());
    assertEquals(latestBlock.getTime(), checkpointSaved.getUpdateTime());
  }

  @Test
  void testUpdateTokenInfoLisNonInitialUpdate_WhenLastUpdateTimeMoreThanADayAgo_ShouldSkipUpdatingTokenInfo() {
    Block latestBlock = Mockito.mock(Block.class);
    when(latestBlock.getBlockNo()).thenReturn(9999L);
    when(latestBlock.getTime()).thenReturn(
        Timestamp.valueOf(LocalDateTime.of(2023, 10, 4, 11, 0, 0)));

    when(blockRepository.findLatestBlock()).thenReturn(Optional.of(latestBlock));

    TokenInfoCheckpoint tokenInfoCheckpoint = Mockito.mock(TokenInfoCheckpoint.class);
    when(tokenInfoCheckpoint.getUpdateTime()).thenReturn(
        Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC).minusDays(3)));
    when(tokenInfoCheckpointRepository.findLatestTokenInfoCheckpoint())
        .thenReturn(Optional.of(tokenInfoCheckpoint));
    tokenInfoService.updateTokenInfoList();

    verifyNoInteractions(tokenInfoRepository);
    verifyNoInteractions(multiAssetRepository);
    verifyNoInteractions(jooqAddressTokenRepository);
    verifyNoInteractions(multiAssetService);
  }
}
