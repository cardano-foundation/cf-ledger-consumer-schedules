package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.consumercommon.entity.StakeAddress;
import org.cardanofoundation.job.projection.StakeAddressProjection;

public interface StakeAddressRepository extends JpaRepository<StakeAddress, Long> {

  StakeAddress findByView(@Param("aLong") String aLong);

  @Query(
      value =
          "SELECT sa.id as id, sa.view as ƯstakeAddress, sum(addr.balance) as totalStake"
              + " FROM StakeAddress sa"
              + " LEFT JOIN Address addr ON addr.stakeAddress = sa"
              + " WHERE EXISTS (SELECT d FROM Delegation d WHERE d.address = sa)"
              + " AND (SELECT max(sr.txId) FROM StakeRegistration sr WHERE sr.addr = sa) >"
              + " (SELECT COALESCE(max(sd.txId), 0) FROM StakeDeregistration sd WHERE sd.addr = sa)"
              + " GROUP BY sa.id"
              + " HAVING sum(addr.balance) IS NOT NULL"
              + " ORDER BY totalStake DESC")
  List<StakeAddressProjection> findStakeAddressOrderByBalance(Pageable pageable);
}
