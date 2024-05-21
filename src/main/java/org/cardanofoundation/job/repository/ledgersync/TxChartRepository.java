package org.cardanofoundation.job.repository.ledgersync;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.common.entity.ledgersync.TxChart;

@Repository
public interface TxChartRepository extends JpaRepository<TxChart, Long> {
  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY tx_chart", nativeQuery = true)
  void refreshMaterializedView();
}
