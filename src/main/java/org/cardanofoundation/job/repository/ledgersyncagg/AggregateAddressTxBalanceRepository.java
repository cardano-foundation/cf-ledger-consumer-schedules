package org.cardanofoundation.job.repository.ledgersyncagg;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.common.entity.compositeKey.AggAddressTxBalanceId;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.AggregateAddressTxBalance;

public interface AggregateAddressTxBalanceRepository
    extends JpaRepository<AggregateAddressTxBalance, AggAddressTxBalanceId> {

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(
      value = "REFRESH MATERIALIZED VIEW CONCURRENTLY agg_address_tx_balance",
      nativeQuery = true)
  void refreshMaterializedView();
}
