package org.cardanofoundation.job.repository.ledgersync;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.common.entity.ledgersync.TxCount;

public interface TxCountRepository extends JpaRepository<TxCount, Long> {
  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = "REFRESH MATERIALIZED VIEW tx_count", nativeQuery = true)
  void refreshMaterializedView();
}
