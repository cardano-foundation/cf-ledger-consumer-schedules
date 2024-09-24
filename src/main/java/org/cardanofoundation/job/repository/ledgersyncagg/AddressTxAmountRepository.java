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
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.StakeAddressTxCount;
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
              WHERE ata.slot > :fromSlot AND ata.slot <= :toSlot AND ata.address IS NOT NULL
              GROUP BY ata.address
              """)
  List<AddressTxCount> getTotalTxCountByAddressInSlotRange(
      @Param("fromSlot") Long fromSlot, @Param("toSlot") Long toSlot);

  @Query(
      """
        SELECT new org.cardanofoundation.explorer.common.entity.ledgersyncsagg.StakeAddressTxCount(ata.stakeAddress, count(distinct(ata.txHash)))
        FROM AddressTxAmount ata
        WHERE ata.slot > :fromSlot AND ata.slot <= :toSlot AND ata.stakeAddress IS NOT NULL
        GROUP BY ata.stakeAddress
""")
  List<StakeAddressTxCount> getTotalTxCountByStakeAddressInSlotRange(
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

  @Query(value = "select max(slot) from cursor_", nativeQuery = true)
  Long getMaxSlotNoFromCursor();
}
