package org.cardanofoundation.job.mapper;

import com.bloxbean.cardano.client.util.AssetUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.cardanofoundation.explorer.consumercommon.entity.AssetMetadata;
import org.cardanofoundation.job.dto.AssetMetadataDTO;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public abstract class AssetMedataMapper {

  @Mapping(target = "subject", source = "subject")
  @Mapping(target = "name", source = "name.value")
  @Mapping(target = "description", source = "description.value")
  @Mapping(target = "policy", source = "policy")
  @Mapping(target = "ticker", source = "ticker.value")
  @Mapping(target = "url", source = "url.value")
  @Mapping(target = "logo", ignore = true)
  @Mapping(target = "decimals", source = "decimals.value")
  @Mapping(target = "fingerprint", source = "subject", qualifiedByName = "generateFingerprint")
  public abstract AssetMetadata fromDTO(AssetMetadataDTO dto);

  @Named("generateFingerprint")
  protected String generateFingerprint(String subject) {
    // policy is first 56 bytes
    String policyHex = subject.substring(0, 56);
    // name is remaining bytes of subject
    String assetNameHex = subject.substring(56);
    return AssetUtil.calculateFingerPrint(policyHex, assetNameHex);
  }
}
