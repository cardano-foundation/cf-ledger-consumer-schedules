package org.cardanofoundation.job.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.PoolHistoryCheckpoint;

@Repository
public interface PoolHistoryCheckpointRepository
    extends JpaRepository<PoolHistoryCheckpoint, Long> {

  @Query(
      "SELECT COUNT(cp.id) FROM PoolHistoryCheckpoint cp "
          + "WHERE cp.view IN :poolViews AND cp.epochCheckpoint = "
          + "(SELECT max(e.no) - 1 FROM Epoch e) AND cp.earnedReward = TRUE")
  Integer checkRewardByPoolViewAndEpoch(@Param("poolViews") Set<String> poolViews);
}
