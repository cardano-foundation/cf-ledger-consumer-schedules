package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.ledgersync.StakeAddress;

public interface StakeAddressRepository extends JpaRepository<StakeAddress, Long> {

  StakeAddress findByView(@Param("aLong") String aLong);

  @Query(
      value =
          """
      select sa from StakeAddress sa
      where sa.view in :addresses and not exists (select true from StakeAddress sa2 where sa2.view = sa.view and sa2.id > sa.id)
  """)
  List<StakeAddress> findStakeAddressesByViewIn(@Param("addresses") Set<String> addresses);
}
