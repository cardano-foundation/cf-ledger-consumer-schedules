package org.cardanofoundation.job.repository.ledgersyncagg;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.compositeKey.AddressTxAmountId;
import org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.AddressTxAmount;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.projection.StakeTxProjection;
import org.cardanofoundation.job.projection.UniqueAccountTxCountProjection;

public interface AddressTxAmountRepository
    extends JpaRepository<AddressTxAmount, AddressTxAmountId> {

  @Query(
      value =
          """
      select (case when ata.stake_address is null then ata.address else ata.stake_address end) as account, count(distinct(ata.tx_hash)) as txCount
      from address_tx_amount ata
      where ata.epoch = :epochNo
      and ata.slot != -1
      group by account
      """,
      nativeQuery = true)
  List<UniqueAccountTxCountProjection> findUniqueAccountsInEpoch(@Param("epochNo") Integer epochNo);

  @Query(
      value =
          "SELECT new org.cardanofoundation.job.projection.StakeTxProjection(addTxAmount.txHash, sum(addTxAmount.quantity), addTxAmount.blockTime)"
              + " FROM AddressTxAmount addTxAmount"
              + " WHERE addTxAmount.stakeAddress = :stakeAddress"
              + " AND addTxAmount.blockTime >= :fromDate AND addTxAmount.blockTime <= :toDate"
              + " GROUP BY addTxAmount.txHash, addTxAmount.blockTime")
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

  @Query(
      value =
          """
      SELECT new org.cardanofoundation.job.model.TokenVolume(ata.unit, sum(ata.quantity))
      FROM AddressTxAmount ata
      WHERE ata.unit IN :units
      AND ata.blockTime >= :blockTime
      AND ata.quantity > 0
      GROUP BY ata.unit
      """)
  List<TokenVolume> sumBalanceAfterBlockTime(
      @Param("units") List<String> units, @Param("blockTime") Long blockTime);

  @Query(
      value =
          """
      SELECT new org.cardanofoundation.job.model.TokenVolume(ata.unit, sum(ata.quantity))
      FROM AddressTxAmount ata
      WHERE ata.unit IN :units
      AND ata.quantity > 0
      GROUP BY ata.unit
      """)
  List<TokenVolume> getTotalVolumeByUnits(@Param("units") List<String> units);

  @Query(
      value =
          """
      SELECT new org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount(ata.unit, count(distinct (ata.txHash)))
      FROM AddressTxAmount ata
      WHERE ata.unit in :units
      GROUP BY ata.unit
      """)
  List<TokenTxCount> getTotalTxCountByUnitIn(@Param("units") List<String> units);

  @Query(
      "select distinct addressTxAmount.unit "
          + " from AddressTxAmount addressTxAmount"
          + " where addressTxAmount.blockTime >= :fromTime and addressTxAmount.blockTime <= :toTime"
          + " and addressTxAmount.unit != 'lovelace'")
  List<String> getTokensInTransactionInTimeRange(
      @Param("fromTime") Long fromTime, @Param("toTime") Long toTime);

  @Query(
      "select distinct addressTxAmount.unit "
          + " from AddressTxAmount addressTxAmount"
          + " where addressTxAmount.blockNumber >= :fromBlockNo and addressTxAmount.blockNumber <= :toBlockNo"
          + " and addressTxAmount.unit != 'lovelace'")
  List<String> getTokensInTransactionInBlockRange(
      @Param("fromBlockNo") Long fromBlockNo, @Param("toBlockNo") Long toBlockNo);

  @Query(
      value =
          """
          SELECT DISTINCT(SUBSTRING(ata.unit, 1 , 56)) FROM AddressTxAmount ata
          WHERE ata.blockTime >= :fromTime AND ata.blockTime <= :toTime
      """)
  List<String> findPolicyByBlockTimeInRange(
      @Param("fromTime") Long fromTime, @Param("toTime") Long toTime);

  @Query(
      value =
          """
          SELECT DISTINCT(ata.unit) FROM AddressTxAmount ata
          WHERE ata.blockTime >= :fromTime AND ata.blockTime <= :toTime
      """)
  List<String> findUnitByBlockTimeInRange(
      @Param("fromTime") Long fromTime, @Param("toTime") Long toTime);
}
