package org.cardanofoundation.job.dto.govActionMetaData;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Anchor {
  String anchorUrl;
  String anchorHash;
}
