package org.cardanofoundation.job.repository.ledgersync;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.compositeKey.AddressTxAmountId;
import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTxAmount;
import org.cardanofoundation.job.projection.StakeTxProjection;
import org.cardanofoundation.job.projection.UniqueAccountTxCountProjection;

public interface AddressTxAmountRepository
    extends JpaRepository<AddressTxAmount, AddressTxAmountId> {

  @Query(
      """
      select (case when ata.stakeAddress is null then ata.address else ata.stakeAddress end) as account, count(distinct(ata.txHash)) as txCount
      from AddressTxAmount ata
      where ata.epoch = :epochNo
      group by account
      """)
  List<UniqueAccountTxCountProjection> findUniqueAccountsInEpoch(@Param("epochNo") Integer epochNo);


  @Query(
      value =
          "SELECT new org.cardanofoundation.job.projection.StakeTxProjection(tx.id, sum(addTxAmount.quantity), addTxAmount.blockTime)"
              + " FROM AddressTxAmount addTxAmount"
              + " JOIN Tx tx on tx.hash = addTxAmount.txHash"
              + " WHERE addTxAmount.stakeAddress = :stakeAddress"
              + " AND addTxAmount.blockTime >= :fromDate AND addTxAmount.blockTime <= :toDate"
              + " GROUP BY addTxAmount.txHash, addTxAmount.blockTime"
              + " ORDER BY addTxAmount.blockTime DESC")
  Page<StakeTxProjection> findTxAndAmountByStake(
      @Param("stakeAddress") String stakeAddress,
      @Param("fromDate") Long fromDate,
      @Param("toDate") Long toDate,
      Pageable pageable);


  @Query(
      "SELECT COUNT(DISTINCT addTxAmount.txHash) FROM AddressTxAmount addTxAmount"
          + " WHERE addTxAmount.stakeAddress = :stakeAddress"
          + " AND addTxAmount.blockTime >= :fromDate AND addTxAmount.blockTime <= :toDate")
  Long getCountTxByStakeInDateRange(
      @Param("stakeAddress") String stakeAddress,
      @Param("fromDate") Long fromDate,
      @Param("toDate") Long toDate);

}
