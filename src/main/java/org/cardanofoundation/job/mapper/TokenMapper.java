package org.cardanofoundation.job.mapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.cardanofoundation.explorer.consumercommon.entity.MultiAsset;
import org.cardanofoundation.job.dto.token.TokenFilterDto;
import org.cardanofoundation.job.util.HexUtils;

@Mapper(
    componentModel = "spring",
    imports = {HexUtils.class},
    uses = {AssetMedataMapper.class})
public interface TokenMapper {

  @Mapping(
      target = "displayName",
      expression = "java(HexUtils.fromHex(multiAsset.getName(), multiAsset.getFingerprint()))")
  @Mapping(target = "createdOn", source = "time")
  TokenFilterDto fromMultiAssetToFilterDto(MultiAsset multiAsset);

  default LocalDateTime fromTimestamp(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }
}
