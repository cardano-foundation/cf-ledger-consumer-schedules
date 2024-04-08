package org.cardanofoundation.job.repository.ledgersync;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.compositeKey.VotingProcedureId;
import org.cardanofoundation.explorer.common.entity.ledgersync.VotingProcedure;

public interface VotingProcedureRepository
    extends JpaRepository<VotingProcedure, VotingProcedureId> {
  @Query(value = "SELECT lvp.slot FROM VotingProcedure lvp ORDER BY lvp.slot DESC LIMIT 1")
  Optional<Long> findLatestSlotOfVotingProcedure();

  Slice<VotingProcedure> getVotingProcedureBySlotIsGreaterThanEqual(
      @Param("slot") Long slot, Pageable pageable);
}
