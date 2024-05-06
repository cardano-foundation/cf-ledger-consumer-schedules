package org.cardanofoundation.job.repository.ledgersync;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTxCount;

public interface AddressTxCountRepository extends JpaRepository<AddressTxCount, Long> {
  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = "REFRESH MATERIALIZED VIEW address_tx_count", nativeQuery = true)
  void refreshMaterializedView();
}
