package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.common.entity.ledgersync.PoolInfo;
import org.cardanofoundation.job.projection.PoolInfoProjection;

@Repository
public interface PoolInfoRepository extends JpaRepository<PoolInfo, Long> {
  @Query(
      value =
          "select pi.poolId as poolId, coalesce(pi.activeStake,0) as activeStake from PoolInfo pi where pi.fetchedAtEpoch = :epochNo")
  List<PoolInfoProjection> findAllByEpochNo(@Param("epochNo") Integer epochNo);
}
