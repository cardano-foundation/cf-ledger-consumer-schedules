package org.cardanofoundation.job.repository.ledgersync;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.common.entity.ledgersync.ParamProposal;

@Repository
public interface ParamProposalRepository extends JpaRepository<ParamProposal, Long> {

  @Query(value = "select pp.drepActivity from ParamProposal pp where pp.epochNo = :epochNo")
  Optional<Long> findDRepActivityByEpochNo(@Param("epochNo") Long epochNo);
}
