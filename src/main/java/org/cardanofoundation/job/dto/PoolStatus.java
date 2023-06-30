package org.cardanofoundation.job.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

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
