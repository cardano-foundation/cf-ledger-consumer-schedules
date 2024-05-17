package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;
import java.util.Set;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.compositeKey.StakeAddressBalanceId;
import org.cardanofoundation.explorer.common.entity.ledgersync.LatestStakeAddressBalance;

public interface LatestStakeAddressBalanceRepository
    extends JpaRepository<LatestStakeAddressBalance, StakeAddressBalanceId> {

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(
      value = "REFRESH MATERIALIZED VIEW CONCURRENTLY latest_stake_address_balance",
      nativeQuery = true)
  void refreshMaterializedView();

  @Query("SELECT sab FROM LatestStakeAddressBalance sab WHERE sab.address IN :addresses")
  List<LatestStakeAddressBalance> findAllByStakeAddressIn(
      @Param("addresses") Set<String> addresses);
}
