package org.cardanofoundation.job.repository.ledgersyncagg;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.TopStakeAddressBalance;

public interface TopStakeAddressBalanceRepository
    extends JpaRepository<TopStakeAddressBalance, String> {

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(
      value = "REFRESH MATERIALIZED VIEW CONCURRENTLY top_stake_address_balance",
      nativeQuery = true)
  void refreshMaterializedView();
}
