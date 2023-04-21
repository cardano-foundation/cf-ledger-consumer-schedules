package com.sotatek.cardano.job.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetMetadataProperty {
  private String value;
  private String sequenceNumber;
  private Object[] signatures;
}
