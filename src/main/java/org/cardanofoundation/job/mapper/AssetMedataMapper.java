package org.cardanofoundation.job.mapper;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.cardanofoundation.explorer.consumercommon.entity.AssetMetadata;
import org.cardanofoundation.job.dto.AssetMetadataDTO;
import org.cardanofoundation.job.dto.token.TokenMetadataDto;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public abstract class AssetMedataMapper {

  @Value("${application.token-logo-endpoint}")
  protected String tokenLogoEndpoint;

  @Mapping(target = "subject", source = "subject")
  @Mapping(target = "name", source = "name.value")
  @Mapping(target = "description", source = "description.value")
  @Mapping(target = "policy", source = "policy")
  @Mapping(target = "ticker", source = "ticker.value")
  @Mapping(target = "url", source = "url.value")
  @Mapping(target = "logo", ignore = true)
  @Mapping(target = "decimals", source = "decimals.value")
  public abstract AssetMetadata fromDTO(AssetMetadataDTO dto);

  @Mapping(target = "logo", source = "logo", qualifiedByName = "getTokenLogoURL")
  public abstract TokenMetadataDto fromAssetMetadata(AssetMetadata metadata);

  @Named("getTokenLogoURL")
  String getTokenLogoEndpoint(String logo){
    return Objects.isNull(logo) ? null : (tokenLogoEndpoint + logo);
  }
}
