package org.cardanofoundation.job.service.impl;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.consumercommon.entity.AssetMetadata;
import org.cardanofoundation.explorer.consumercommon.entity.MultiAsset;
import org.cardanofoundation.job.dto.BaseFilterDto;
import org.cardanofoundation.job.dto.token.TokenFilterDto;
import org.cardanofoundation.job.mapper.AssetMedataMapper;
import org.cardanofoundation.job.mapper.TokenMapper;
import org.cardanofoundation.job.projection.TokenNumberHoldersProjection;
import org.cardanofoundation.job.projection.TokenVolumeProjection;
import org.cardanofoundation.job.repository.*;
import org.cardanofoundation.job.service.TokenService;
import org.cardanofoundation.job.util.StreamUtil;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

  private final AssetMetadataRepository assetMetadataRepository;
  private final MultiAssetRepository multiAssetRepository;

  private final AddressTokenRepository addressTokenRepository;
  private final TxRepository txRepository;
  private final AddressTokenBalanceRepository addressTokenBalanceRepository;

  private final TokenMapper tokenMapper;
  private final AssetMedataMapper assetMetadataMapper;

  @Override
  @Transactional(readOnly = true)
  public BaseFilterDto<TokenFilterDto> filterToken(Pageable pageable) {
    Page<MultiAsset> multiAssets = multiAssetRepository.findAll(pageable);
    Set<String> subjects =
        StreamUtil.mapApplySet(multiAssets.getContent(), ma -> ma.getPolicy() + ma.getName());
    List<AssetMetadata> assetMetadataList = assetMetadataRepository.findBySubjectIn(subjects);
    Map<String, AssetMetadata> assetMetadataMap =
        StreamUtil.toMap(assetMetadataList, AssetMetadata::getSubject);
    var multiAssetResponsesList = multiAssets.map(tokenMapper::fromMultiAssetToFilterDto);
    Timestamp yesterday =
        Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).minusDays(1));
    Long txId = txRepository.findMinTxByAfterTime(yesterday).orElse(Long.MAX_VALUE);
    List<TokenVolumeProjection> volumes =
        addressTokenRepository.sumBalanceAfterTx(multiAssets.getContent(), txId);

    List<Long> multiAssetIds = StreamUtil.mapApply(multiAssets.getContent(), MultiAsset::getId);
    var mapNumberHolder = getMapNumberHolder(multiAssetIds);
    var tokenVolumeMap =
        StreamUtil.toMap(
            volumes, TokenVolumeProjection::getIdent, TokenVolumeProjection::getVolume);
    multiAssetResponsesList.forEach(
        ma -> {
          ma.setMetadata(
              assetMetadataMapper.fromAssetMetadata(
                  assetMetadataMap.get(ma.getPolicy() + ma.getName())));
          if (tokenVolumeMap.containsKey(ma.getId())) {
            ma.setVolumeIn24h(tokenVolumeMap.get(ma.getId()).toString());
          } else {
            ma.setVolumeIn24h(String.valueOf(0));
          }
          ma.setVolumeIn24h(
              tokenVolumeMap.getOrDefault(ma.getId(), BigInteger.valueOf(0)).toString());
          ma.setNumberOfHolders(mapNumberHolder.get(ma.getId()));
          ma.setId(null);
        });
    return new BaseFilterDto<>(multiAssetResponsesList);
  }

  private Map<Long, Long> getMapNumberHolder(List<Long> multiAssetIds) {
    var numberOfHoldersWithStakeKey =
        addressTokenBalanceRepository.countByMultiAssetIn(multiAssetIds);
    var numberOfHoldersWithAddressNotHaveStakeKey =
        addressTokenBalanceRepository.countAddressNotHaveStakeByMultiAssetIn(multiAssetIds);

    var numberHoldersStakeKeyMap =
        StreamUtil.toMap(
            numberOfHoldersWithStakeKey,
            TokenNumberHoldersProjection::getIdent,
            TokenNumberHoldersProjection::getNumberOfHolders);
    var numberHoldersAddressNotHaveStakeKeyMap =
        StreamUtil.toMap(
            numberOfHoldersWithAddressNotHaveStakeKey,
            TokenNumberHoldersProjection::getIdent,
            TokenNumberHoldersProjection::getNumberOfHolders);
    return multiAssetIds.stream()
        .collect(
            Collectors.toMap(
                ident -> ident,
                ident ->
                    numberHoldersStakeKeyMap.getOrDefault(ident, 0L)
                        + numberHoldersAddressNotHaveStakeKeyMap.getOrDefault(ident, 0L)));
  }
}
