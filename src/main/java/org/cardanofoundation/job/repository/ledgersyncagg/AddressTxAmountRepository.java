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
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.AddressTxCount;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.projection.StakeTxProjection;
import org.cardanofoundation.job.projection.UniqueAccountTxCountProjection;

public interface AddressTxAmountRepository
    extends JpaRepository<AddressTxAmount, AddressTxAmountId> {

  @Query(
      value =
          """
          SELECT (CASE WHEN ata.stake_address IS NULL THEN ata.address ELSE ata.stake_address END) AS account,
                 COUNT(DISTINCT ata.tx_hash)                                                       AS txCount
          FROM address_tx_amount ata
          WHERE ata.epoch = :epochNo
            AND ata.slot != -1
          GROUP BY account;
      """,
      nativeQuery = true)
  List<UniqueAccountTxCountProjection> findUniqueAccountsInEpoch(@Param("epochNo") Integer epochNo);

  @Query(
      """
      SELECT new org.cardanofoundation.job.projection.StakeTxProjection(addTxAmount.txHash, sum(addTxAmount.quantity), addTxAmount.blockTime)
      FROM AddressTxAmount addTxAmount
      WHERE addTxAmount.stakeAddress = :stakeAddress
        AND addTxAmount.slot >= :fromSlot
        AND addTxAmount.slot <= :toSlot
        AND addTxAmount.unit = 'lovelace'
      GROUP BY addTxAmount.txHash, addTxAmount.blockTime
      """)
  Page<StakeTxProjection> findTxAndAmountByStakeAndSlotRange(
      @Param("stakeAddress") String stakeAddress,
      @Param("fromSlot") Long fromSlot,
      @Param("toSlot") Long toSlot,
      Pageable pageable);

  @Query(
      """
      SELECT COUNT(DISTINCT addTxAmount.txHash)
      FROM AddressTxAmount addTxAmount
      WHERE addTxAmount.stakeAddress = :stakeAddress
        AND addTxAmount.unit = 'lovelace'
        AND addTxAmount.slot >= :fromSlot
        AND addTxAmount.slot <= :toSlot
      """)
  Long getCountTxByStakeInDateRange(
      @Param("stakeAddress") String stakeAddress,
      @Param("fromSlot") Long fromSlot,
      @Param("toSlot") Long toSlot);

  @Query(
      """
      SELECT new org.cardanofoundation.job.model.TokenVolume(ata.unit, sum(ata.quantity))
      FROM AddressTxAmount ata
      WHERE ata.unit IN :units
        AND ata.slot >= :toSlot
        AND ata.quantity > 0
      GROUP BY ata.unit
      """)
  List<TokenVolume> sumBalanceAfterSlot(
      @Param("units") List<String> units, @Param("toSlot") Long toSlot);

  @Query(
      """
      SELECT new org.cardanofoundation.job.model.TokenVolume(ata.unit, sum(ata.quantity))
      FROM AddressTxAmount ata
      WHERE ata.unit IN :units
      AND ata.quantity > 0
      AND ata.slot > :fromSlot AND ata.slot <= :toSlot
      GROUP BY ata.unit
      """)
  List<TokenVolume> getTotalVolumeByUnits(
      @Param("units") List<String> units,
      @Param("fromSlot") Long fromSlot,
      @Param("toSlot") Long toSlot);

  @Query(
      """
      SELECT new org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount(ata.unit, count(distinct (ata.txHash)))
      FROM AddressTxAmount ata
      WHERE ata.unit in :units
      GROUP BY ata.unit
      """)
  List<TokenTxCount> getTotalTxCountByUnitIn(@Param("units") List<String> units);

  @Query(
      """
          SELECT new org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount(ata.unit, count(distinct (ata.txHash)))
          FROM AddressTxAmount ata
          WHERE ata.slot > :fromSlot AND ata.slot <= :toSlot
          AND ata.unit != 'lovelace'
          GROUP BY ata.unit
          """)
  List<TokenTxCount> getTotalTxCountByUnitInSlotRange(
      @Param("fromSlot") Long fromSlot, @Param("toSlot") Long toSlot);

  @Query(
      """
              SELECT new org.cardanofoundation.explorer.common.entity.ledgersyncsagg.AddressTxCount(ata.address, count(distinct(ata.txHash)))
              FROM AddressTxAmount ata
              WHERE ata.slot > :fromSlot AND ata.slot <= :toSlot
              GROUP BY ata.address
              """)
  List<AddressTxCount> getTotalTxCountByAddressInSlotRange(
      @Param("fromSlot") Long fromSlot, @Param("toSlot") Long toSlot);

  @Query(
      """
      SELECT addressTxAmount.unit
      FROM AddressTxAmount addressTxAmount
      WHERE addressTxAmount.blockTime >= :fromTime
        AND addressTxAmount.blockTime <= :toTime
        AND addressTxAmount.unit != 'lovelace'
      """)
  List<String> getTokensInTransactionInTimeRange(
      @Param("fromTime") Long fromTime, @Param("toTime") Long toTime);

  @Query(
      """
          SELECT distinct addressTxAmount.unit
          FROM AddressTxAmount addressTxAmount
          WHERE addressTxAmount.slot > :fromSlot
            AND addressTxAmount.slot <= :toSlot
            AND addressTxAmount.unit != 'lovelace'
          """)
  List<String> getTokensInTransactionInSlotRange(
      @Param("fromSlot") Long fromSlot, @Param("toSlot") Long toSlot);

  @Query(
      """
      SELECT DISTINCT(SUBSTRING(ata.unit, 1 , 56)) FROM AddressTxAmount ata
      WHERE ata.slot >= :fromSlot AND ata.slot <= :toSlot
      """)
  List<String> findPolicyBySlotInRange(
      @Param("fromSlot") Long fromSlot, @Param("toSlot") Long toSlot);

  @Query(
      """
      SELECT DISTINCT(ata.unit) FROM AddressTxAmount ata
      WHERE ata.unit != 'lovelace'
      AND ata.slot >= :fromSlot AND ata.slot <= :toSlot
      """)
  List<String> findUnitBySlotInRange(
      @Param("fromSlot") Long fromSlot, @Param("toSlot") Long toSlot);

  @Query(
      """
      SELECT DISTINCT(ata.stakeAddress) FROM AddressTxAmount ata
      WHERE ata.slot >= :fromSlot AND ata.slot <= :toSlot
      AND ata.stakeAddress IS NOT NULL
      """)
  List<String> findStakeAddressBySlotNoBetween(
      @Param("fromSlot") Long fromSlot, @Param("toSlot") Long toSlot);

  @Query(
      """
      SELECT DISTINCT(ata.address) FROM AddressTxAmount ata
      WHERE ata.slot >= :fromSlot AND ata.slot <= :toSlot
      """)
  List<String> findAddressBySlotNoBetween(
      @Param("fromSlot") Long fromSlot, @Param("toSlot") Long toSlot);

  @Query(value = "SELECT max(a.id) FROM Address a")
  Long getMaxAddressId();

  @Query(value = "select max(slot) from cursor_", nativeQuery = true)
  Long getMaxSlotNoFromCursor();
}
