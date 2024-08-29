package org.cardanofoundation.job.repository.ledgersyncagg;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.StakeAddressTxCount;

public interface StakeAddressTxCountRepository extends JpaRepository<StakeAddressTxCount, String> {
  List<StakeAddressTxCount> findAllByStakeAddressIn(Collection<String> stakeAddresses);
}
