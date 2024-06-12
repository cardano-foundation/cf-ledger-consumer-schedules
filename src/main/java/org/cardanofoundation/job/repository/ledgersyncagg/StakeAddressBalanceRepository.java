package org.cardanofoundation.job.repository.ledgersyncagg;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.common.entity.compositeKey.StakeAddressBalanceId;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.StakeAddressBalance;
import org.cardanofoundation.job.projection.StakeBalanceProjection;

public interface StakeAddressBalanceRepository
    extends JpaRepository<StakeAddressBalance, StakeAddressBalanceId> {

  @Query(
      value =
          """
                  SELECT sab.address, sab.quantity
                  FROM stake_address_view sav
                  CROSS JOIN LATERAL ( SELECT tmp.address,
                                        tmp.quantity
                                 FROM stake_address_balance tmp
                                 WHERE tmp.address = sav.stake_address
                                 ORDER BY tmp.slot DESC
                                 LIMIT 1) sab
                  WHERE sav.stake_address IN :stakeAddresses
              """,
      nativeQuery = true)
  List<StakeBalanceProjection> findStakeAddressBalanceByStakeAddressIn(
      @Param("stakeAddresses") Collection<String> stakeAddresses);

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY stake_address_view", nativeQuery = true)
  void refreshStakeAddressMaterializedView();
}
