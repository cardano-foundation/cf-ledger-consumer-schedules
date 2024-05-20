package org.cardanofoundation.job.mapper;

import lombok.extern.log4j.Log4j2;

import com.bloxbean.cardano.client.util.AssetUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import org.cardanofoundation.explorer.common.entity.ledgersync.AssetMetadata;
import org.cardanofoundation.job.dto.AssetMetadataDTO;

@Mapper(componentModel = "spring")
@Log4j2
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
    try {
      String fingerprint = AssetUtil.calculateFingerPrint(policyHex, assetNameHex);
      return fingerprint;
    } catch (Exception e) {
      log.error("Error generating fingerprint for asset metadata: {}", e.getMessage());
      return null;
    }
  }
}
