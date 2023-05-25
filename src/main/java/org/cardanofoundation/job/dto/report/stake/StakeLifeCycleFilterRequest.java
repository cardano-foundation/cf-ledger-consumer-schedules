package org.cardanofoundation.job.dto.report.stake;

import java.sql.Timestamp;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@EqualsAndHashCode
public class StakeLifeCycleFilterRequest {

  private Timestamp fromDate;
  private Timestamp toDate;
}
