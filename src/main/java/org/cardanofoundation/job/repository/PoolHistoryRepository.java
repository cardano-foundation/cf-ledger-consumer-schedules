package org.cardanofoundation.job.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.PoolHistory;
import org.cardanofoundation.job.projection.PoolHistoryKoiOsProjection;

@Repository
public interface PoolHistoryRepository extends JpaRepository<PoolHistory, Long> {

  @Query(
      value =
          "SELECT ph.epochNo AS epochNo, CAST(ph.delegRewards AS BigInteger) AS delegateReward, ph.epochRos AS ros, "
              + "CAST(ph.activeStake AS BigInteger) AS activeStake, CAST(ph.poolFees AS BigInteger) AS poolFees "
              + "FROM PoolHistory ph "
              + "WHERE ph.poolId = :poolId "
              + "AND ph.epochNo between :epochBegin and :epochEnd "
              + "ORDER BY ph.epochNo DESC")
  List<PoolHistoryKoiOsProjection> getPoolHistoryKoiOs(
      @Param("poolId") String poolId,
      @Param("epochBegin") int epochBegin,
      @Param("epochEnd") int epochEnd);
}
