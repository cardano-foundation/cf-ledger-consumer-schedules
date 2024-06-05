package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.compositeKey.AddressTxAmountId;
import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTxAmount;
import org.cardanofoundation.job.model.TokenTxCount;
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
          "SELECT new org.cardanofoundation.job.projection.StakeTxProjection(tx.id, sum(addTxAmount.quantity), addTxAmount.blockTime)"
              + " FROM AddressTxAmount addTxAmount"
              + " JOIN Tx tx on tx.hash = addTxAmount.txHash"
              + " WHERE addTxAmount.stakeAddress = :stakeAddress"
              + " AND addTxAmount.blockTime >= :fromDate AND addTxAmount.blockTime <= :toDate"
              + " AND addTxAmount.unit = 'lovelace'"
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
      value =
          """
          WITH block_start AS
                  (
                   SELECT extract(epoch from b.time)\\:\\:integer as block_time
                   FROM block b INNER JOIN tx ON tx.block_id = b.id
                   WHERE tx.id = :txId
                  )
                SELECT ma.id AS ident, sum(ata.quantity) AS volume
                FROM address_tx_amount ata
                JOIN multi_asset ma
                  ON ata.unit = ma.unit
                WHERE ma.id between :startIdent AND :endIdent
                  AND ata.quantity > 0
                  AND ata.block_time > (SELECT block_time FROM block_start)
                GROUP BY ma.id
      """, nativeQuery = true)
  List<org.cardanofoundation.job.model.projection.TokenVolume> sumBalanceAfterTx(
      @Param("startIdent") Long startIdent,
      @Param("endIdent") Long endIdent,
      @Param("txId") Long txId);

  @Query(
      value =
          """
      SELECT new org.cardanofoundation.job.model.TokenVolume(ma.id, sum(ata.quantity))
      FROM AddressTxAmount ata
      JOIN MultiAsset ma ON ata.unit = ma.unit
      JOIN Tx tx ON tx.hash = ata.txHash
      WHERE ma.id IN :multiAssetIds
      AND tx.id >= :txId
      AND ata.quantity > 0
      GROUP BY ma.id
      """)
  List<TokenVolume> sumBalanceAfterTx(
      @Param("multiAssetIds") List<Long> multiAssetIds, @Param("txId") Long txId);

  @Query(
      value =
          """
      SELECT new org.cardanofoundation.job.model.TokenVolume(ma.id, sum(ata.quantity))
      FROM AddressTxAmount ata
      JOIN MultiAsset ma ON ata.unit = ma.unit
      WHERE ma.id >= :startIdent AND ma.id <= :endIdent
      AND ata.quantity > 0
      GROUP BY ma.id
      """)
  List<TokenVolume> getTotalVolumeByIdentInRange(
      @Param("startIdent") Long startIdent, @Param("endIdent") Long endIdent);

  @Query(
      value =
          """
      SELECT new org.cardanofoundation.job.model.TokenTxCount(ma.id, count(distinct (ata.txHash)))
      FROM AddressTxAmount ata
      JOIN MultiAsset ma ON ata.unit = ma.unit
      WHERE ma.id >= :startIdent AND ma.id <= :endIdent
      GROUP BY ma.id
      """)
  List<TokenTxCount> getTotalTxCountByIdentInRange(
      @Param("startIdent") Long startIdent, @Param("endIdent") Long endIdent);

  @Query(
      value =
          """
      SELECT new org.cardanofoundation.job.model.TokenVolume(ma.id, sum(ata.quantity))
      FROM AddressTxAmount ata
      JOIN MultiAsset ma ON ata.unit = ma.unit
      WHERE ma.id IN :multiAssetIds
      AND ata.quantity > 0
      GROUP BY ma.id
      """)
  List<TokenVolume> getTotalVolumeByIdentIn(@Param("multiAssetIds") List<Long> multiAssetIds);

  @Query(
      value =
          """
      SELECT new org.cardanofoundation.job.model.TokenTxCount(ma.id, count(distinct (ata.txHash)))
      FROM AddressTxAmount ata
      JOIN MultiAsset ma ON ata.unit = ma.unit
      WHERE ma.id IN :multiAssetIds
      GROUP BY ma.id
      """)
  List<TokenTxCount> getTotalTxCountByIdentIn(@Param("multiAssetIds") List<Long> multiAssetIds);
}
