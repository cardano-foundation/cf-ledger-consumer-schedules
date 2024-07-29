package org.cardanofoundation.job.repository.ledgersyncagg;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.StakeAddressTxCount;

public interface StakeAddressTxCountRepository extends JpaRepository<StakeAddressTxCount, String> {
  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(
      value = "REFRESH MATERIALIZED VIEW CONCURRENTLY stake_address_tx_count",
      nativeQuery = true)
  void refreshMaterializedView();
}
