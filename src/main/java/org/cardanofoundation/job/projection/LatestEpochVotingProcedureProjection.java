package org.cardanofoundation.job.projection;

public interface LatestEpochVotingProcedureProjection {
  Long getEpoch();

  String getVoterHash();
}
