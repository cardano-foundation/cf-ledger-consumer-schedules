package org.cardanofoundation.job.repository.ledgersync.aggregate;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.common.entity.ledgersync.aggregation.AggregateAddressTxBalance;

public interface AggregateAddressTxBalanceRepository
    extends JpaRepository<AggregateAddressTxBalance, Long> {

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY agg_address_tx_balance", nativeQuery = true)
  void refreshMaterializedView();
}
