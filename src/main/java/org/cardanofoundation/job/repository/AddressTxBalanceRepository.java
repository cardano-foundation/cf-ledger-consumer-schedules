package org.cardanofoundation.job.repository;

import java.sql.Timestamp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import org.cardanofoundation.explorer.consumercommon.entity.AddressTxBalance;
import org.cardanofoundation.job.projection.StakeTxProjection;


public interface AddressTxBalanceRepository extends JpaRepository<AddressTxBalance, Long> {

  @Query(value = "SELECT addrTxBalance.tx.id as txId, sum(addrTxBalance.balance) as amount,"
      + " addrTxBalance.time as time"
      + " FROM AddressTxBalance addrTxBalance"
      + " WHERE addrTxBalance.address IN "
      + " (SELECT addr FROM Address addr WHERE addr.stakeAddress.view = :stakeAddress)"
      + " AND addrTxBalance.time >= :fromDate AND addrTxBalance.time <= :toDate"
      + " GROUP BY addrTxBalance.tx.id, addrTxBalance.time")
  Page<StakeTxProjection> findTxAndAmountByStake(@Param("stakeAddress") String stakeAddress,
                                                 @Param("fromDate") Timestamp fromDate,
                                                 @Param("toDate") Timestamp toDate,
                                                 Pageable pageable);

}
