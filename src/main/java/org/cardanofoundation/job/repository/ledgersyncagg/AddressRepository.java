package org.cardanofoundation.job.repository.ledgersyncagg;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {

  @Query("SELECT a.address FROM Address a")
  Slice<String> getAddressBySlice(Pageable pageable);
}
