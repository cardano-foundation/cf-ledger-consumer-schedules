package org.cardanofoundation.job.dto;

import java.util.Set;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class PoolStatus {
  Set<Long> poolActivateIds;

  Set<Long> poolInactivateIds;
}
