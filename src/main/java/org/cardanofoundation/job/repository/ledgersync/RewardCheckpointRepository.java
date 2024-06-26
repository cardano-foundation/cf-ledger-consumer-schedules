package org.cardanofoundation.job.repository.ledgersync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.ledgersync.RewardCheckpoint;

public interface RewardCheckpointRepository extends JpaRepository<RewardCheckpoint, Long> {

  @Query(
      "SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM RewardCheckpoint r "
          + "WHERE r.stakeAddress = :stakeAddress AND r.epochCheckpoint = "
          + "(SELECT max(epoch.no) - 1 FROM Epoch epoch)")
  boolean checkRewardByStakeAddressAndEpoch(@Param("stakeAddress") String stakeAddress);
}
