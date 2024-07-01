package org.cardanofoundation.job.repository.ledgersyncagg;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.common.entity.compositeKey.AggAddressTokenId;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.AggregateAddressToken;

public interface AggregateAddressTokenRepository
    extends JpaRepository<AggregateAddressToken, AggAddressTokenId> {

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY agg_address_token", nativeQuery = true)
  void refreshMaterializedView();
}
