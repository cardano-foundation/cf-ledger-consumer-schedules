package org.cardanofoundation.job.dto.token;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenMetadataDto {
  private String url;
  private String ticker;
  private Integer decimals;
  private String logo;
  private String description;
}
