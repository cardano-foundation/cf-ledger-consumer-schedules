package org.cardanofoundation.job.repository.ledgersync;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.common.entity.ledgersync.StakeTxBalance;

public interface StakeTxBalanceRepository extends JpaRepository<StakeTxBalance, Long> {
  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = "REFRESH MATERIALIZED VIEW stake_tx_balance", nativeQuery = true)
  void refreshMaterializedView();
}
