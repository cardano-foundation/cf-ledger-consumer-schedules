package org.cardanofoundation.job.repository.ledgersync.aggregate;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.common.entity.ledgersync.aggregation.AggregateAddressToken;

public interface AggregateAddressTokenRepository
    extends JpaRepository<AggregateAddressToken, Long> {

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = "REFRESH MATERIALIZED VIEW agg_address_token", nativeQuery = true)
  void refreshMaterializedView();
}
