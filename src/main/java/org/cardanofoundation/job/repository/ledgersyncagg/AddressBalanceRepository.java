package org.cardanofoundation.job.repository.ledgersyncagg;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.common.entity.compositeKey.AddressBalanceId;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.AddressBalance;

public interface AddressBalanceRepository extends JpaRepository<AddressBalance, AddressBalanceId> {

  @Query(value = "SELECT MAX(ab.slot) FROM AddressBalance ab")
  Long getMaxSlot();
}
