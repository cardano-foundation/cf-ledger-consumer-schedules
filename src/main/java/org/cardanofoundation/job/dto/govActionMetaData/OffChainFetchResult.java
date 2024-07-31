package org.cardanofoundation.job.dto.govActionMetaData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class OffChainFetchResult {

  String anchorUrl;
  String anchorHash;
  String rawData;
  private boolean isValid;
  private boolean isFetchSuccess;
  private String fetchFailError;
}
