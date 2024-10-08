package org.cardanofoundation.job.repository.ledgersync;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.compositeKey.GovActionProposalId;
import org.cardanofoundation.explorer.common.entity.enumeration.GovActionType;
import org.cardanofoundation.explorer.common.entity.ledgersync.GovActionProposal;
import org.cardanofoundation.job.dto.govActionMetaData.Anchor;
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

  @Query(value = "select gap from GovActionProposal gap where gap.type in (:govActionTypes)")
  List<GovActionProposal> getGovActionThatAllowedToVoteForSPO(
      @Param("govActionTypes") Collection<GovActionType> govActionTypes);

  @Query(
      """
        SELECT new org.cardanofoundation.job.dto.govActionMetaData.Anchor(gap.anchorUrl, gap.anchorHash)
        FROM GovActionProposal gap
        WHERE gap.slot > :fromSlot and gap.slot <= :toSlot
        AND gap.anchorUrl IS NOT NULL
        AND gap.anchorHash IS NOT NULL
        AND NOT EXISTS (SELECT 1 FROM OffChainVoteGovActionData oc WHERE oc.anchorHash = gap.anchorHash AND oc.anchorUrl = gap.anchorUrl)
        """)
  List<Anchor> getAnchorInfoBySlotRange(
      @Param("fromSlot") Long fromSlot, @Param("toSlot") Long toSlot);

  @Query(
      """
        SELECT new org.cardanofoundation.job.dto.govActionMetaData.Anchor(ocvfe.anchorUrl, ocvfe.anchorHash)
        FROM OffChainVoteFetchError ocvfe
        WHERE ocvfe.retryCount < :retryCount
          AND NOT EXISTS (SELECT 1
                          FROM OffChainVoteGovActionData ocvgad
                          WHERE ocvgad.anchorUrl = ocvfe.anchorUrl
                            AND ocvgad.anchorHash = ocvfe.anchorHash)
        """)
  List<Anchor> getAnchorInfoByRetryCount(@Param("retryCount") Integer retryCount);

  @Query(
      value =
          """
        SELECT MAX(gap.slot) as maxSlotNo
        FROM GovActionProposal gap
        """)
  Optional<Long> maxSlotNo();
}
