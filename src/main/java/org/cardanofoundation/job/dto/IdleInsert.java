package org.cardanofoundation.job.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class IdleInsert {
  private int size;
  private int retry;
}
