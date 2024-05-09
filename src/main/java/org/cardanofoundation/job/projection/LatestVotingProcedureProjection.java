package org.cardanofoundation.job.projection;

import org.cardanofoundation.explorer.common.entity.enumeration.Vote;

public interface LatestVotingProcedureProjection {
  String getGovActionTxHash();

  Integer getGovActionIdx();

  String getVoterHash();

  Vote getVote();

  Long getPoolId();

  Long getSlotGov();
}
