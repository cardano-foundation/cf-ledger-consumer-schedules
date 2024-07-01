package org.cardanofoundation.job.repository.ledgersyncagg;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.TopAddressBalance;

public interface TopAddressBalanceRepository extends JpaRepository<TopAddressBalance, String> {

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY top_address_balance", nativeQuery = true)
  void refreshMaterializedView();
}
