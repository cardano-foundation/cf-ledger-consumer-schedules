package org.cardanofoundation.job.repository.ledgersync;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.ledgersync.VotingProcedure;
import org.cardanofoundation.explorer.common.entity.ledgersync.compositeKey.VotingProcedureId;

public interface VotingProcedureRepository
    extends JpaRepository<VotingProcedure, VotingProcedureId> {

  Slice<VotingProcedure> getVotingProcedureBySlotIsGreaterThanEqual(
      @Param("slot") Long slot, Pageable pageable);
}
