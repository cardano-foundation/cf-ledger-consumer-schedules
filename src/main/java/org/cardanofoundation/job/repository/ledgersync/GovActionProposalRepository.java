package org.cardanofoundation.job.repository.ledgersync;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.ledgersync.GovActionProposal;
import org.cardanofoundation.explorer.common.entity.ledgersync.compositeKey.GovActionProposalId;
import org.cardanofoundation.job.projection.gov.GovActionVoteCountProjection;

public interface GovActionProposalRepository
    extends JpaRepository<GovActionProposal, GovActionProposalId> {

  @Query(
      value =
          "SELECT gap.id AS govActionProposalId, gap.epoch as gapEpochNo, ep as epochParam, "
              + "SUM(CASE WHEN vp.vote = 'YES' AND vp.voterType = 'DREP_KEY_HASH' THEN 1 ELSE 0 END) as numberOfDRepYesVotes, "
              + "SUM(CASE WHEN vp.vote = 'NO' AND vp.voterType = 'DREP_KEY_HASH' THEN 1 ELSE 0 END) as numberOfDRepNoVotes, "
              + "SUM(CASE WHEN vp.vote = 'ABSTAIN' AND vp.voterType = 'DREP_KEY_HASH' THEN 1 ELSE 0 END) as numberOfDRepAbstainVotes, "
              + "SUM(CASE WHEN vp.vote = 'YES' AND vp.voterType = 'STAKING_POOL_KEY_HASH' THEN 1 ELSE 0 END) as numberOfPoolYesVotes, "
              + "SUM(CASE WHEN vp.vote = 'NO' AND vp.voterType = 'STAKING_POOL_KEY_HASH' THEN 1 ELSE 0 END) as numberOfPoolNoVotes, "
              + "SUM(CASE WHEN vp.vote = 'ABSTAIN' AND vp.voterType = 'STAKING_POOL_KEY_HASH' THEN 1 ELSE 0 END) as numberOfPoolAbstainVotes,"
              + "SUM(CASE WHEN vp.vote = 'YES' AND vp.voterType = 'CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH' THEN 1 ELSE 0 END) as numberOfCCRepYesVotes, "
              + "SUM(CASE WHEN vp.vote = 'NO' AND vp.voterType = 'CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH' THEN 1 ELSE 0 END) as numberOfCCRepNoVotes, "
              + "SUM(CASE WHEN vp.vote = 'ABSTAIN' AND vp.voterType = 'CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH' THEN 1 ELSE 0 END) as numberOfCCRepAbstainVotes "
              + "FROM GovActionProposal gap "
              + "JOIN EpochParam ep ON gap.epoch = ep.epochNo "
              + "LEFT JOIN LatestVotingProcedure vp ON gap.txHash = vp.govActionTxHash AND gap.index = vp.govActionIndex "
              + "WHERE gap.id IN (:govActionProposalIds)"
              + "GROUP BY govActionProposalId, ep.id")
  List<GovActionVoteCountProjection> findGovActionVoteCountByGovActionProposalIdIn(
      @Param("govActionProposalIds") Collection<GovActionProposalId> govActionProposalIds);
}
