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

  @Query(value = "SELECT max(block_number) FROM cursor_", nativeQuery = true)
  Long getMaxBlockNoFromCursor();

  @Query(
      value =
          """
                    SELECT (CASE WHEN ata.stake_address IS NULL THEN ata.address ELSE ata.stake_address END) AS account, count(DISTINCT(ata.tx_hash)) AS txCount
                    FROM address_tx_amount ata
                    WHERE ata.epoch = :epochNo
                    AND ata.slot != -1
                    GROUP BY account
                    """,
      nativeQuery = true)
  List<UniqueAccountTxCountProjection> findUniqueAccountsInEpoch(@Param("epochNo") Integer epochNo);

  @Query(
      value =
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
      value =
          """
                    SELECT COUNT(DISTINCT addTxAmount.txHash) FROM AddressTxAmount addTxAmount
                    WHERE addTxAmount.stakeAddress = :stakeAddress
                    AND addTxAmount.blockTime >= :fromDate AND addTxAmount.blockTime <= :toDate
                    """)
  Long getCountTxByStakeInDateRange(
      @Param("stakeAddress") String stakeAddress,
      @Param("fromDate") Long fromDate,
      @Param("toDate") Long toDate);

  @Query(
      value =
          """
                    SELECT DISTINCT addressTxAmount.unit
                    FROM AddressTxAmount addressTxAmount
                    WHERE addressTxAmount.blockNumber >= :fromBlockNo and addressTxAmount.blockNumber <= :toBlockNo
                    AND addressTxAmount.unit != 'lovelace'
                    """)
  List<String> getTokensInTransactionInBlockRange(
      @Param("fromBlockNo") Long fromBlockNo, @Param("toBlockNo") Long toBlockNo);

  @Query(
      value =
          """
                    SELECT DISTINCT addressTxAmount.unit
                    FROM AddressTxAmount addressTxAmount
                    WHERE addressTxAmount.blockTime >= :fromTime and addressTxAmount.blockTime <= :toTime
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
}
