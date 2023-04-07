package com.sotatek.cardano.job.mapper;

import com.sotatek.cardano.common.entity.AssetMetadata;
import com.sotatek.cardano.job.dto.AssetMetadataDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
