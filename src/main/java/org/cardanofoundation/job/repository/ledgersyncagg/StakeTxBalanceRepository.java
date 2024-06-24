package org.cardanofoundation.job.repository.ledgersyncagg;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.common.entity.compositeKey.StakeAddressTxBalanceId;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.StakeTxBalance;

public interface StakeTxBalanceRepository
    extends JpaRepository<StakeTxBalance, StakeAddressTxBalanceId> {
  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY stake_tx_balance", nativeQuery = true)
  void refreshMaterializedView();
}
