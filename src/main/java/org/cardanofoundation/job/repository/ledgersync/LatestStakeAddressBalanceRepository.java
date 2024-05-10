package org.cardanofoundation.job.repository.ledgersync;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.common.entity.compositeKey.AddressBalanceId;
import org.cardanofoundation.explorer.common.entity.compositeKey.StakeAddressBalanceId;
import org.cardanofoundation.explorer.common.entity.ledgersync.LatestAddressBalance;
import org.cardanofoundation.explorer.common.entity.ledgersync.LatestStakeAddressBalance;

public interface LatestStakeAddressBalanceRepository
    extends JpaRepository<LatestStakeAddressBalance, StakeAddressBalanceId> {

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY latest_stake_address_balance", nativeQuery = true)
  void refreshMaterializedView();
}
