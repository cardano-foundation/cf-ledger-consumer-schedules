package org.cardanofoundation.job.dto.govActionMetaData;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OffChainGovActionData {

  private String anchorUrl;
  private String anchorHash;
  private String rawData;
  private String title;
  private String abstractContent;
  private String motivation;
  private String rationale;
  private boolean isValid;
  private boolean isFetchSuccess;
  private String fetchFailReason;
}
