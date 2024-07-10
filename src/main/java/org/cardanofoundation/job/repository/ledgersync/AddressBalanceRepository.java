package org.cardanofoundation.job.repository.ledgersync;

import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.common.entity.compositeKey.AddressBalanceId;
import org.cardanofoundation.explorer.common.entity.ledgersync.AddressBalance;

public interface AddressBalanceRepository extends JpaRepository<AddressBalance, AddressBalanceId> {

  Optional<AddressBalance> findFirstBy(Sort sort);
}
