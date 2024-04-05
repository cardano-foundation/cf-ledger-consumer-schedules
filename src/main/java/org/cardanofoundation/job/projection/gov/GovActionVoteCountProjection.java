package org.cardanofoundation.job.projection.gov;

import org.cardanofoundation.explorer.common.entity.compositeKey.GovActionProposalId;
import org.cardanofoundation.explorer.common.entity.ledgersync.EpochParam;

public interface GovActionVoteCountProjection {
  GovActionProposalId getGovActionProposalId();

  Integer getGapEpochNo();

  EpochParam getEpochParam();

  Long getNumberOfDRepYesVotes();

  Long getNumberOfDRepNoVotes();

  Long getNumberOfDRepAbstainVotes();

  Long getNumberOfPoolYesVotes();

  Long getNumberOfPoolNoVotes();

  Long getNumberOfPoolAbstainVotes();

  Long getNumberOfCCRepYesVotes();

  Long getNumberOfCCRepNoVotes();

  Long getNumberOfCCRepAbstainVotes();
}
