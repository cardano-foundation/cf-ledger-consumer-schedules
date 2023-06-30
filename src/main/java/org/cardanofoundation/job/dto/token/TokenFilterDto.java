package org.cardanofoundation.job.dto.token;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class TokenFilterDto {
  private Long id;
  private String name;
  private String displayName;
  private String policy;
  private String fingerprint;
  private Integer txCount;
  private String supply;
  private String volumeIn24h;
  private String totalVolume;
  private Long numberOfHolders;
  private LocalDateTime createdOn;
  private TokenMetadataDto metadata;
}
