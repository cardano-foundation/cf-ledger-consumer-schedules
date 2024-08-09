package org.cardanofoundation.job.dto.govActionMetaData;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anchor {
  String anchorUrl;
  String anchorHash;
}
