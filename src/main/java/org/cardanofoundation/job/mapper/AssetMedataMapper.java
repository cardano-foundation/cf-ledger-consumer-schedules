package org.cardanofoundation.job.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.cardanofoundation.explorer.consumercommon.entity.AssetMetadata;
import org.cardanofoundation.job.dto.AssetMetadataDTO;

@Mapper(componentModel = "spring")
public interface AssetMedataMapper {

  @Mapping(target = "subject", source = "subject")
  @Mapping(target = "name", source = "name.value")
  @Mapping(target = "description", source = "description.value")
  @Mapping(target = "policy", source = "policy")
  @Mapping(target = "ticker", source = "ticker.value")
  @Mapping(target = "url", source = "url.value")
  @Mapping(target = "logo", source = "logo.value")
  @Mapping(target = "decimals", source = "decimals.value")
  AssetMetadata fromDTO(AssetMetadataDTO dto);
}
