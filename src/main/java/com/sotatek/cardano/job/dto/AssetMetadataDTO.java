package com.sotatek.cardano.job.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
