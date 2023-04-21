package com.sotatek.cardano.job.dto;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetMetadataDTO {
  private String subject;
  private AssetMetadataProperty name;
  private AssetMetadataProperty description;
  private String policy;
  private AssetMetadataProperty ticker;
  private AssetMetadataProperty url;
  private AssetMetadataProperty logo;
  private AssetMetadataProperty decimals;
}
