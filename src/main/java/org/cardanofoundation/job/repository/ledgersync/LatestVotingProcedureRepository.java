package org.cardanofoundation.job.repository.ledgersync;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.compositeKey.LatestVotingProcedureId;
import org.cardanofoundation.explorer.common.entity.enumeration.VoterType;
import org.cardanofoundation.explorer.common.entity.ledgersync.LatestVotingProcedure;
import org.cardanofoundation.job.projection.LatestDrepVotingProcedureProjection;
import org.cardanofoundation.job.projection.LatestEpochVotingProcedureProjection;
import org.cardanofoundation.job.projection.LatestVotingProcedureProjection;

public interface LatestVotingProcedureRepository
    extends JpaRepository<LatestVotingProcedure, LatestVotingProcedureId> {

  @Query(value = "SELECT lvp.slot FROM LatestVotingProcedure lvp ORDER BY lvp.slot DESC LIMIT 1")
  Optional<Long> findLatestSlotOfVotingProcedure();

  @Query(
      value =
          "select max(lvp.epoch) as epoch, lvp.voterHash as voterHash from LatestVotingProcedure lvp "
              + " where lvp.voterType = 'DREP_KEY_HASH' and lvp.epoch >= :fromEpoch"
              + " and lvp.voterHash in :dRepHashes"
              + " group by lvp.voterHash")
  List<LatestEpochVotingProcedureProjection> findAllByVoterHashAndEpochNo(
      @Param("fromEpoch") Long fromEpoch, @Param("dRepHashes") Set<String> dRepHashes);

  @Query(
      value =
          "SELECT lvp FROM LatestVotingProcedure lvp "
              + "WHERE lvp.votingProcedureId IN :votingProcedureIds")
  List<LatestVotingProcedure> getAllByIdIn(
      @Param("votingProcedureIds") Collection<LatestVotingProcedureId> votingProcedureIds);

  @Query(
      value =
          "SELECT lvp.voterHash as voterHash, lvp.govActionTxHash as govActionTxHash, lvp.govActionIndex as govActionIndex,"
              + " lvp.vote as vote, ph.id as poolId, gap.slot as slotGov"
              + " FROM LatestVotingProcedure lvp"
              + " join GovActionProposal gap on gap.txHash = lvp.govActionTxHash and gap.index = lvp.govActionIndex "
              + " join PoolHash ph on lvp.voterHash = ph.hashRaw"
              + " WHERE lvp.voterType = :voterType")
  List<LatestVotingProcedureProjection> findAllByVoterType(@Param("voterType") VoterType voterType);

  @Query(
      value =
          "SELECT lvp.voterHash as voterHash, lvp.govActionTxHash as govActionTxHash, lvp.govActionIndex as govActionIndex,"
              + " lvp.vote as vote, gap.blockTime as blockTime"
              + " FROM LatestVotingProcedure lvp"
              + " join GovActionProposal gap on (gap.txHash = lvp.govActionTxHash and gap.index = lvp.govActionIndex) "
              + " WHERE lvp.voterHash in :drepIds")
  List<LatestDrepVotingProcedureProjection> findAllByVoterHashIn(
      @Param("drepIds") Collection<String> drepIds);
}
