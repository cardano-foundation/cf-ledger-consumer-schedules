package org.cardanofoundation.job.repository.ledgersync;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.common.entity.compositeKey.StakeAddressBalanceId;
import org.cardanofoundation.explorer.common.entity.ledgersync.StakeAddressBalance;

public interface StakeAddressBalanceRepository extends
    JpaRepository<StakeAddressBalance, StakeAddressBalanceId> {
}
