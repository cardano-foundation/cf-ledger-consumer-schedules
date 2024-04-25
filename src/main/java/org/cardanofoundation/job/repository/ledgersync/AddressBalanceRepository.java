package org.cardanofoundation.job.repository.ledgersync;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.common.entity.compositeKey.AddressBalanceId;
import org.cardanofoundation.explorer.common.entity.ledgersync.AddressBalance;

public interface AddressBalanceRepository extends JpaRepository<AddressBalance, AddressBalanceId> {

}
