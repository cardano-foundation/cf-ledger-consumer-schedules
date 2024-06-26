package org.cardanofoundation.job.dto.report.pool;

import java.math.BigInteger;
import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.cardanofoundation.job.projection.LifeCycleRewardProjection;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RewardResponse {

  private Integer epochNo;

  private Timestamp time;

  private BigInteger amount;

  private String rewardAccount;

  public RewardResponse(LifeCycleRewardProjection projection) {
    this.epochNo = projection.getEpochNo();
    this.time = projection.getTime();
    this.amount = projection.getAmount();
    this.rewardAccount = projection.getAddress();
  }
}
