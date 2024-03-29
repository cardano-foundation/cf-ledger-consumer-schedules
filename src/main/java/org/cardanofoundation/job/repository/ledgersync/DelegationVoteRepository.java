package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.ledgersync.DelegationVote;
import org.cardanofoundation.explorer.common.entity.ledgersync.compositeKey.DelegationVoteId;
import org.cardanofoundation.job.projection.DelegationVoteProjection;

public interface DelegationVoteRepository extends JpaRepository<DelegationVote, DelegationVoteId> {
  @Query(
      value =
          "select dv.drepHash as drepHash,dv.address as address,dv.txHash as txHash from DelegationVote dv where dv.drepHash in :dRepHash")
  List<DelegationVoteProjection> findAllByDRepHashIn(@Param("dRepHash") Set<String> dRepHash);
}
