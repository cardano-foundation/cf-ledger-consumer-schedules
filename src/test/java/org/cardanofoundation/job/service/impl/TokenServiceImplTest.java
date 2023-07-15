package org.cardanofoundation.job.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.explorer.consumercommon.entity.AssetMetadata;
import org.cardanofoundation.explorer.consumercommon.entity.MultiAsset;
import org.cardanofoundation.job.dto.token.TokenFilterDto;
import org.cardanofoundation.job.dto.token.TokenMetadataDto;
import org.cardanofoundation.job.mapper.AssetMedataMapper;
import org.cardanofoundation.job.mapper.TokenMapper;
import org.cardanofoundation.job.projection.TokenNumberHoldersProjectionImpl;
import org.cardanofoundation.job.repository.AddressTokenBalanceRepository;
import org.cardanofoundation.job.repository.AddressTokenRepository;
import org.cardanofoundation.job.repository.AssetMetadataRepository;
import org.cardanofoundation.job.repository.MultiAssetRepository;
import org.cardanofoundation.job.repository.TxRepository;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

  @Mock MultiAssetRepository multiAssetRepository;
  @Mock AssetMetadataRepository assetMetadataRepository;
  @Mock AddressTokenBalanceRepository addressTokenBalanceRepository;
  @Mock AddressTokenRepository addressTokenRepository;
  @Mock TxRepository txRepository;
  @InjectMocks TokenServiceImpl tokenService;
  @Mock TokenMapper tokenMapper;
  @Mock AssetMedataMapper assetMetadataMapper;

  @Test
  void exportTokenReport_shouldTokenListSuccess() {
    // Init value
    Pageable pageable = PageRequest.of(0, 10);

    MultiAsset multiAsset =
        MultiAsset.builder()
            .id(1734731L)
            .fingerprint("asset17q7r59zlc3dgw0venc80pdv566q6yguw03f0d9")
            .name("HOSKY")
            .nameView("HOSKY")
            .policy("a0028f350aaabe0545fdcb56b039bfb08e4bb4d8c4d7c3c7d481c235")
            .totalVolume(BigInteger.valueOf(74405760743875966L))
            .time(Timestamp.valueOf(LocalDateTime.of(2021, 11, 5, 11, 15, 13)))
            .build();
    List<MultiAsset> multiAssets = List.of(multiAsset);

    TokenFilterDto tokenFilterDto = new TokenFilterDto();
    tokenFilterDto.setPolicy("a0028f350aaabe0545fdcb56b039bfb08e4bb4d8c4d7c3c7d481c235");
    tokenFilterDto.setName("HOSKY");
    tokenFilterDto.setTotalVolume("74405760743875966");

    AssetMetadata assetMetadata = new AssetMetadata();
    assetMetadata.setSubject("a0028f350aaabe0545fdcb56b039bfb08e4bb4d8c4d7c3c7d481c235HOSKY");
    assetMetadata.setName("HOSKY");
    assetMetadata.setPolicy("a0028f350aaabe0545fdcb56b039bfb08e4bb4d8c4d7c3c7d481c235");

    Long txId = Long.MAX_VALUE;

    when(multiAssetRepository.findAll(pageable)).thenReturn(new PageImpl(multiAssets));
    when(tokenMapper.fromMultiAssetToFilterDto(multiAsset)).thenReturn(tokenFilterDto);
    when(assetMetadataRepository.findBySubjectIn(any())).thenReturn(List.of(assetMetadata));
    when(txRepository.findMinTxByAfterTime(any())).thenReturn(txId.describeConstable());
    when(addressTokenRepository.sumBalanceAfterTx(any(), any())).thenReturn(new ArrayList<>());
    when(addressTokenBalanceRepository.countByMultiAssetIn(any())).thenReturn(new ArrayList<>());
    when(addressTokenBalanceRepository.countAddressNotHaveStakeByMultiAssetIn(any()))
        .thenReturn(new ArrayList<>());
    when(assetMetadataMapper.fromAssetMetadata(any())).thenReturn(new TokenMetadataDto());

    var response = tokenService.filterToken(pageable);

    Assertions.assertEquals(1, response.getTotalItems());
    Assertions.assertEquals("HOSKY", response.getData().get(0).getName());
    Assertions.assertEquals(
        "a0028f350aaabe0545fdcb56b039bfb08e4bb4d8c4d7c3c7d481c235",
        response.getData().get(0).getPolicy());
    Assertions.assertEquals("74405760743875966", response.getData().get(0).getTotalVolume());
  }

  @Test
  void exportTokenReport_getMapNumberHolder_shouldReturnMapNumberHolderByMultiAssetIds()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    var multiAssetIds = List.of(1L, 2L, 3L, 4L);
    TokenNumberHoldersProjectionImpl t1 = new TokenNumberHoldersProjectionImpl(1L, 100L);
    TokenNumberHoldersProjectionImpl t2 = new TokenNumberHoldersProjectionImpl(2L, 200L);
    TokenNumberHoldersProjectionImpl t3 = new TokenNumberHoldersProjectionImpl(3L, 300L);

    when(addressTokenBalanceRepository.countByMultiAssetIn(multiAssetIds))
        .thenReturn(List.of(t1, t2, t3));
    when(addressTokenBalanceRepository.countAddressNotHaveStakeByMultiAssetIn(multiAssetIds))
        .thenReturn(List.of(t1, t3));

    Method method = TokenServiceImpl.class.getDeclaredMethod("getMapNumberHolder", List.class);
    method.setAccessible(true);
    Map<Long, Long> mapNumberHolder = (Map<Long, Long>) method.invoke(tokenService, multiAssetIds);
    Assertions.assertEquals(200L, mapNumberHolder.get(1L));
    Assertions.assertEquals(200L, mapNumberHolder.get(2L));
    Assertions.assertEquals(600L, mapNumberHolder.get(3L));
    Assertions.assertEquals(0L, mapNumberHolder.get(4L));
  }
}
