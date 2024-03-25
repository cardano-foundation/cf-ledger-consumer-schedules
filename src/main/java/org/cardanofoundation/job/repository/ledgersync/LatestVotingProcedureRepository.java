package org.cardanofoundation.job.repository.ledgersync;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.ledgersync.LatestVotingProcedure;
import org.cardanofoundation.explorer.common.entity.ledgersync.compositeKey.LatestVotingProcedureId;

public interface LatestVotingProcedureRepository
    extends JpaRepository<LatestVotingProcedure, LatestVotingProcedureId> {

  @Query(value = "SELECT lvp.slot FROM LatestVotingProcedure lvp ORDER BY lvp.slot DESC LIMIT 1")
  Optional<Long> findLatestSlotOfVotingProcedure();

  @Query(
      value =
          "SELECT lvp FROM LatestVotingProcedure lvp "
              + "WHERE lvp.votingProcedureId IN :votingProcedureIds")
  List<LatestVotingProcedure> getAllByIdIn(
      @Param("votingProcedureIds") Collection<LatestVotingProcedureId> votingProcedureIds);
}
