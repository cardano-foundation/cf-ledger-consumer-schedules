package org.cardanofoundation.job.repository.ledgersync;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.common.entity.ledgersync.EpochStake;
import org.cardanofoundation.job.projection.PoolReportProjection;

@Repository
public interface EpochStakeRepository extends JpaRepository<EpochStake, Long> {

  @Query(
      value =
          "SELECT es.epochNo as epochNo, sum(es.amount) as size "
              + "FROM EpochStake es "
              + "where es.pool.view = :poolView "
              + "and es.epochNo between :epochBegin and :epochEnd "
              + "group by es.epochNo")
  Page<PoolReportProjection> getEpochSizeByPoolReport(
      @Param("poolView") String poolView,
      @Param("epochBegin") int epochBegin,
      @Param("epochEnd") int epochEnd,
      Pageable pageable);
}
