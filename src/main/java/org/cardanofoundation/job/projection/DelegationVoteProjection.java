package org.cardanofoundation.job.projection;

public interface DelegationVoteProjection {
  String getDrepHash();

  String getAddress();

  String getTxHash();
}
