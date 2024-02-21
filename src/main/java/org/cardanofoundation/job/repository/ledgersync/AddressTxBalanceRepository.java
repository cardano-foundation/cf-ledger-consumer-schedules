package org.cardanofoundation.job.repository.ledgersync;

import java.sql.Timestamp;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTxBalance;
import org.cardanofoundation.job.projection.StakeTxProjection;

public interface AddressTxBalanceRepository extends JpaRepository<AddressTxBalance, Long> {

  @Query(
      value =
          "SELECT new org.cardanofoundation.job.projection.StakeTxProjection("
              + "addrTxBalance.tx.id, sum(addrTxBalance.balance), addrTxBalance.time)"
              + " FROM AddressTxBalance addrTxBalance"
              + " WHERE addrTxBalance.address IN "
              + " (SELECT addr FROM Address addr WHERE addr.stakeAddress.view = :stakeAddress)"
              + " AND addrTxBalance.time >= :fromDate AND addrTxBalance.time <= :toDate"
              + " GROUP BY addrTxBalance.tx.id, addrTxBalance.time"
              + " ORDER BY addrTxBalance.time DESC")
  Page<StakeTxProjection> findTxAndAmountByStake(
      @Param("stakeAddress") String stakeAddress,
      @Param("fromDate") Timestamp fromDate,
      @Param("toDate") Timestamp toDate,
      Pageable pageable);

  @Query(
      "SELECT COUNT(DISTINCT addrTxBalance.txId) FROM AddressTxBalance addrTxBalance"
          + " WHERE addrTxBalance.address IN "
          + " (SELECT addr FROM Address addr WHERE addr.stakeAddress.view = :stakeAddress)"
          + " AND addrTxBalance.time >= :fromDate AND addrTxBalance.time <= :toDate")
  Long getCountTxByStakeInDateRange(
      @Param("stakeAddress") String stakeAddress,
      @Param("fromDate") Timestamp fromDate,
      @Param("toDate") Timestamp toDate);

  @Query("select max(tx.time) from AddressTxBalance tx")
  Optional<Timestamp> getMaxTime();
}
