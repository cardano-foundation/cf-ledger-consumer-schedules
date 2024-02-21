package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.common.entity.ledgersync.PoolHistory;
import org.cardanofoundation.job.projection.PoolHistoryKoiOsProjection;

@Repository
public interface PoolHistoryRepository extends JpaRepository<PoolHistory, Long> {

  @Query(
      value =
          "SELECT ph.epochNo AS epochNo, ph.delegatorRewards AS delegateReward, ph.epochRos AS ros, "
              + "ph.activeStake AS activeStake, ph.poolFees AS poolFees "
              + "FROM PoolHistory ph "
              + "WHERE ph.pool.view = :poolView "
              + "AND ph.epochNo between :epochBegin and :epochEnd "
              + "ORDER BY ph.epochNo DESC")
  List<PoolHistoryKoiOsProjection> getPoolHistoryKoiOs(
      @Param("poolView") String poolView,
      @Param("epochBegin") int epochBegin,
      @Param("epochEnd") int epochEnd);
}
