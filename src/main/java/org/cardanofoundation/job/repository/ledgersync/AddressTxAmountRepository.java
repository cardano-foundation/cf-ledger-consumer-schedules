package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.compositeKey.AddressTxAmountId;
import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTxAmount;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.projection.StakeTxProjection;
import org.cardanofoundation.job.projection.UniqueAccountTxCountProjection;

public interface AddressTxAmountRepository
    extends JpaRepository<AddressTxAmount, AddressTxAmountId> {

  @Query(value = "select max(block_number) from cursor_", nativeQuery = true)
  Long getMaxBlockNoFromCursor();

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
          "SELECT new org.cardanofoundation.job.projection.StakeTxProjection(tx.id, sum(addTxAmount.quantity), addTxAmount.blockTime)"
              + " FROM AddressTxAmount addTxAmount"
              + " JOIN Tx tx on tx.hash = addTxAmount.txHash"
              + " WHERE addTxAmount.stakeAddress = :stakeAddress"
              + " AND addTxAmount.blockTime >= :fromDate AND addTxAmount.blockTime <= :toDate"
              + " GROUP BY addTxAmount.txHash, addTxAmount.blockTime, tx.id")
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
      "select distinct addressTxAmount.unit "
          + " from AddressTxAmount addressTxAmount"
          + " where addressTxAmount.blockNumber >= :fromBlockNo and addressTxAmount.blockNumber <= :toBlockNo"
          + " and addressTxAmount.unit != 'lovelace'")
  List<String> getTokensInTransactionInBlockRange(
      @Param("fromBlockNo") Long fromBlockNo, @Param("toBlockNo") Long toBlockNo);

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
          + " where addressTxAmount.slot BETWEEN :slotFrom and :slotTo "
          + " and addressTxAmount.unit != 'lovelace'")
  List<String> getTokensInTransactionInSlotRange(
      @Param("slotFrom") Long slotFrom, @Param("slotTo") Long slotTo);

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
      AND ata.slot >= :slotFrom
      AND ata.quantity > 0
      GROUP BY ata.unit
      """)
  List<TokenVolume> sumBalanceAfterBlockSlot(
      @Param("units") List<String> units, @Param("slotFrom") Long slotFrom);

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
}
