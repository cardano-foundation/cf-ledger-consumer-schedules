package org.cardanofoundation.job.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.LongStream;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.explorer.common.entity.explorer.TokenInfo;
import org.cardanofoundation.explorer.common.entity.explorer.TokenInfoCheckpoint;
import org.cardanofoundation.explorer.common.entity.ledgersync.Block;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.repository.explorer.TokenInfoCheckpointRepository;
import org.cardanofoundation.job.repository.explorer.TokenInfoRepository;
import org.cardanofoundation.job.repository.explorer.jooq.JOOQTokenInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.MultiAssetRepository;
import org.cardanofoundation.job.repository.ledgersync.TxRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.service.MultiAssetService;
import org.cardanofoundation.job.service.TokenInfoServiceAsync;

@ExtendWith(MockitoExtension.class)
@Disabled
class TokenInfoServiceImplTest {

  @Mock private TokenInfoCheckpointRepository tokenInfoCheckpointRepository;
  @Mock private BlockRepository blockRepository;
  @Mock private MultiAssetRepository multiAssetRepository;
  @Mock private TokenInfoRepository tokenInfoRepository;
  @Mock private TokenInfoServiceAsync tokenInfoServiceAsync;
  @Mock private TxRepository txRepository;
  @Mock private JOOQTokenInfoRepository jooqTokenInfoRepository;
  @Mock private AddressTxAmountRepository addressTxAmountRepository;
  @Mock private MultiAssetService multiAssetService;
  @Mock private RedisTemplate<String, String> redisTemplate;
  @Mock private HashOperations hashOperations;

  @Captor private ArgumentCaptor<List<TokenInfo>> tokenInfosCaptor;
  @Captor private ArgumentCaptor<TokenInfoCheckpoint> tokenInfoCheckpointCaptor;

  @InjectMocks private TokenInfoServiceImpl tokenInfoService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(tokenInfoService, "network", "mainnet");
  }

  @Test
  void testUpdateTokenInfoListForFirstTime() {
    Block latestBlock = Mockito.mock(Block.class);
    when(latestBlock.getBlockNo()).thenReturn(10000L);
    when(latestBlock.getTime())
        .thenReturn(Timestamp.valueOf(LocalDateTime.of(2021, 11, 5, 11, 15, 13)));
    when(blockRepository.findLatestBlock()).thenReturn(Optional.of(latestBlock));
    when(tokenInfoCheckpointRepository.findLatestTokenInfoCheckpoint())
        .thenReturn(Optional.empty());
    long multiAssetCount = 180000;
    when(multiAssetRepository.getCurrentMaxIdent()).thenReturn(multiAssetCount);

    when(tokenInfoServiceAsync.buildTokenInfoList(
            anyLong(), anyLong(), anyLong(), anyLong(), any(Timestamp.class)))
        .thenAnswer(
            invocation -> {
              Long startIdent = invocation.getArgument(0);
              Long endIdent = invocation.getArgument(1);
              List<TokenInfo> mockTokenInfoList = new ArrayList<>();
              LongStream.rangeClosed(startIdent, endIdent)
                  .forEach(i -> mockTokenInfoList.add(new TokenInfo()));
              return CompletableFuture.completedFuture(mockTokenInfoList);
            });

    when(multiAssetRepository.count()).thenReturn(multiAssetCount);
    when(redisTemplate.opsForHash()).thenReturn(hashOperations);

    tokenInfoService.updateTokenInfoList();

    verify(jooqTokenInfoRepository, atLeastOnce()).insertAll(tokenInfosCaptor.capture());
    verify(tokenInfoCheckpointRepository, times(1)).save(tokenInfoCheckpointCaptor.capture());
    assertEquals(
        (int) multiAssetCount, tokenInfosCaptor.getAllValues().stream().mapToInt(List::size).sum());
    var tokenInfoCheckpointSaved = tokenInfoCheckpointCaptor.getValue();
    assertEquals(latestBlock.getBlockNo(), tokenInfoCheckpointSaved.getBlockNo());
    assertEquals(latestBlock.getTime(), tokenInfoCheckpointSaved.getUpdateTime());
  }

  @Test
  void testUpdateTokenInfoListForFirstTime_WhenBuildTokenInfoListFailed_ShouldThrowException() {
    Block latestBlock = Mockito.mock(Block.class);
    when(latestBlock.getBlockNo()).thenReturn(10000L);
    when(latestBlock.getTime())
        .thenReturn(Timestamp.valueOf(LocalDateTime.of(2021, 11, 5, 11, 15, 13)));
    when(blockRepository.findLatestBlock()).thenReturn(Optional.of(latestBlock));
    when(tokenInfoCheckpointRepository.findLatestTokenInfoCheckpoint())
        .thenReturn(Optional.empty());
    long multiAssetCount = 180000;
    when(multiAssetRepository.getCurrentMaxIdent()).thenReturn(multiAssetCount);

    when(tokenInfoServiceAsync.buildTokenInfoList(
            anyLong(), anyLong(), anyLong(), anyLong(), any(Timestamp.class)))
        .thenThrow(RuntimeException.class);

    assertThrows(RuntimeException.class, () -> tokenInfoService.updateTokenInfoList());
  }

  @Test
  void testUpdateTokenInfoListNonInitialUpdate() {
    Block latestBlock = Mockito.mock(Block.class);
    when(latestBlock.getBlockNo()).thenReturn(9999L);
    when(latestBlock.getTime())
        .thenReturn(Timestamp.valueOf(LocalDateTime.of(2023, 10, 4, 11, 0, 0)));

    when(blockRepository.findLatestBlock()).thenReturn(Optional.of(latestBlock));

    TokenInfoCheckpoint tokenInfoCheckpoint = Mockito.mock(TokenInfoCheckpoint.class);
    when(tokenInfoCheckpoint.getBlockNo()).thenReturn(9990L);
    when(tokenInfoCheckpoint.getUpdateTime())
        .thenReturn(Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC).minusHours(1)));

    when(tokenInfoCheckpointRepository.findLatestTokenInfoCheckpoint())
        .thenReturn(Optional.of(tokenInfoCheckpoint));

    when(addressTxAmountRepository.getTokensInTransactionInTimeRange(any(), any()))
        .thenReturn(List.of("unit2", "unit3"));

    final TokenVolume tokenVolume1 = new TokenVolume("unit1", BigInteger.valueOf(100L));
    final TokenVolume tokenVolume2 = new TokenVolume("unit2", BigInteger.valueOf(200L));
    final TokenVolume tokenVolume3 = new TokenVolume("unit3", BigInteger.valueOf(300L));
    final List<TokenVolume> tokenVolumes = List.of(tokenVolume1, tokenVolume2, tokenVolume3);
    when(addressTxAmountRepository.sumBalanceAfterBlockTime(anyList(), anyLong()))
        .thenReturn(tokenVolumes);

    when(multiAssetService.getMapNumberHolderByUnits(anyList()))
        .thenReturn(
            Map.ofEntries(
                Map.entry("unit1", 10L), Map.entry("unit2", 20L), Map.entry("unit3", 30L)));

    TokenInfo tokenInfo1 = spy(TokenInfo.class);
    when(tokenInfo1.getMultiAssetId()).thenReturn(1L);
    TokenInfo tokenInfo2 = spy(TokenInfo.class);
    when(tokenInfo2.getMultiAssetId()).thenReturn(2L);
    TokenInfo tokenInfo3 = spy(TokenInfo.class);
    when(tokenInfo3.getMultiAssetId()).thenReturn(3L);
    when(tokenInfoRepository.findByMultiAssetIdIn(anyCollection()))
        .thenReturn(List.of(tokenInfo1, tokenInfo2, tokenInfo3));
    when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    tokenInfoService.updateTokenInfoList();

    verify(tokenInfoRepository).saveAll(tokenInfosCaptor.capture());
    List<TokenInfo> tokenInfosSaved = tokenInfosCaptor.getValue();
    assertThat(tokenInfosSaved)
        .hasSize(3)
        .extracting(
            TokenInfo::getBlockNo,
            TokenInfo::getVolume24h,
            TokenInfo::getNumberOfHolders,
            TokenInfo::getUpdateTime)
        .containsExactlyInAnyOrder(
            tuple(latestBlock.getBlockNo(), tokenVolume1.getVolume(), 10L, latestBlock.getTime()),
            tuple(latestBlock.getBlockNo(), tokenVolume2.getVolume(), 20L, latestBlock.getTime()),
            tuple(latestBlock.getBlockNo(), tokenVolume3.getVolume(), 30L, latestBlock.getTime()));

    verify(tokenInfoCheckpointRepository).save(tokenInfoCheckpointCaptor.capture());
    TokenInfoCheckpoint checkpointSaved = tokenInfoCheckpointCaptor.getValue();
    assertEquals(latestBlock.getBlockNo(), checkpointSaved.getBlockNo());
    assertEquals(latestBlock.getTime(), checkpointSaved.getUpdateTime());
  }

  @Test
  void
      testUpdateTokenInfoLisNonInitialUpdate_WhenMaxBlockNoEqualBlockNoCheckPoint_ShouldSkipUpdatingTokenInfo() {
    Block latestBlock = Mockito.mock(Block.class);
    when(latestBlock.getBlockNo()).thenReturn(9999L);
    when(latestBlock.getTime())
        .thenReturn(Timestamp.valueOf(LocalDateTime.of(2023, 10, 4, 11, 0, 0)));

    when(blockRepository.findLatestBlock()).thenReturn(Optional.of(latestBlock));

    TokenInfoCheckpoint tokenInfoCheckpoint = Mockito.mock(TokenInfoCheckpoint.class);
    when(tokenInfoCheckpoint.getBlockNo()).thenReturn(9999L);
    when(tokenInfoCheckpointRepository.findLatestTokenInfoCheckpoint())
        .thenReturn(Optional.of(tokenInfoCheckpoint));
    when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    tokenInfoService.updateTokenInfoList();

    verifyNoInteractions(tokenInfoRepository);
    verifyNoInteractions(addressTxAmountRepository);
    verifyNoInteractions(multiAssetService);
  }
}
