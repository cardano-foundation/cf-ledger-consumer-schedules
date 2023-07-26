package org.cardanofoundation.job.mapper;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import org.cardanofoundation.job.util.DataUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.cardanofoundation.explorer.consumercommon.entity.AssetMetadata;
import org.cardanofoundation.job.dto.AssetMetadataDTO;
import org.cardanofoundation.job.dto.token.TokenMetadataDto;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AssetMedataMapper {

  @Mapping(target = "subject", source = "subject")
  @Mapping(target = "name", source = "name.value")
  @Mapping(target = "description", source = "description.value")
  @Mapping(target = "policy", source = "policy")
  @Mapping(target = "ticker", source = "ticker.value")
  @Mapping(target = "url", source = "url.value")
  @Mapping(target = "logo", ignore = true)
  @Mapping(target = "decimals", source = "decimals.value")
  @Mapping(target = "logoHash", source = "logo.value", qualifiedByName = "getLogoHash")
  AssetMetadata fromDTO(AssetMetadataDTO dto);

  TokenMetadataDto fromAssetMetadata(AssetMetadata metadata);

  @Named("getLogoHash")
  default String getLogoHash(String base64String) {
    return DataUtil.isNullOrEmpty(base64String) ? null : HexUtil.encodeHexString(
        Blake2bUtil.blake2bHash256(base64String.getBytes()));
  }
}
