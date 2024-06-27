package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.compositeKey.AddressTxAmountId;
import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTxAmount;
import org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.projection.StakeTxProjection;
import org.cardanofoundation.job.projection.UniqueAccountTxCountProjection;

public interface AddressTxAmountRepository
    extends JpaRepository<AddressTxAmount, AddressTxAmountId> {

  @Query(value = "select max(block_number) from cursor_", nativeQuery = true)
  Long getMaxBlockNoFromCursor();

  @Query(value = "select max(slot) from cursor_", nativeQuery = true)
  Long getMaxSlotNoFromCursor();

  @Query(value = "SELECT max(a.id) FROM Address a")
  Long getMaxAddressId();

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
      """
      SELECT new org.cardanofoundation.job.projection.StakeTxProjection(tx.id, sum(addTxAmount.quantity), addTxAmount.blockTime)
         FROM AddressTxAmount addTxAmount
         JOIN Tx tx on tx.hash = addTxAmount.txHash
         WHERE addTxAmount.stakeAddress = :stakeAddress
         AND addTxAmount.blockTime >= :fromDate AND addTxAmount.blockTime <= :toDate
         GROUP BY addTxAmount.txHash, addTxAmount.blockTime, tx.id
    """)
  Page<StakeTxProjection> findTxAndAmountByStake(
      @Param("stakeAddress") String stakeAddress,
      @Param("fromDate") Long fromDate,
      @Param("toDate") Long toDate,
      Pageable pageable);

  @Query(
      """
       SELECT COUNT(DISTINCT addTxAmount.txHash)
           FROM AddressTxAmount addTxAmount
           WHERE addTxAmount.stakeAddress = :stakeAddress
           AND addTxAmount.blockTime >= :fromDate AND addTxAmount.blockTime <= :toDate
       """)
  Long getCountTxByStakeInDateRange(
      @Param("stakeAddress") String stakeAddress,
      @Param("fromDate") Long fromDate,
      @Param("toDate") Long toDate);

  @Query(
      """
        SELECT DISTINCT (addressTxAmount.unit)
           FROM AddressTxAmount addressTxAmount
           WHERE addressTxAmount.blockTime >= :fromTime AND addressTxAmount.blockTime <= :toTime
           AND addressTxAmount.unit != 'lovelace'
      """)
  List<String> getTokensInTransactionInTimeRange(
      @Param("fromTime") Long fromTime, @Param("toTime") Long toTime);

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
      value =
          """
      SELECT DISTINCT(ata.unit) FROM AddressTxAmount ata
      WHERE ata.unit != 'lovelace'
      AND ata.blockTime >= :fromTime AND ata.blockTime <= :toTime
      """)
  List<String> findUnitByBlockTimeInRange(
      @Param("fromTime") Long fromTime, @Param("toTime") Long toTime);

  @Query(
      value =
          """
          SELECT DISTINCT(ata.stakeAddress) FROM AddressTxAmount ata
          WHERE ata.slot >= :fromTime AND ata.slot <= :toTime
          AND ata.stakeAddress IS NOT NULL
      """)
  List<String> findStakeAddressBySlotNoBetween(
      @Param("fromTime") Long fromTime, @Param("toTime") Long toTime);

  @Query(
      value =
          """
          SELECT DISTINCT(ata.address) FROM AddressTxAmount ata
          WHERE ata.blockTime >= :fromTime AND ata.blockTime <= :toTime
      """)
  List<String> findAddressByBlockTimeBetween(
      @Param("fromTime") Long fromTime, @Param("toTime") Long toTime);
}
