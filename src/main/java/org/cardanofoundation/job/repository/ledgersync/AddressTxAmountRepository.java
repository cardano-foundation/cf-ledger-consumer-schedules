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

  @Query(
      value =
          """
      SELECT new org.cardanofoundation.job.model.TokenVolume(ma.id, sum(ata.quantity))
      FROM AddressTxAmount ata
      JOIN MultiAsset ma ON ata.unit = ma.unit
      JOIN Tx tx ON tx.hash = ata.txHash
      WHERE ma.id >= :startIdent AND ma.id <= :endIdent
      AND tx.id >= :txId
      AND ata.quantity > 0
      GROUP BY ma.id
      """)
  List<TokenVolume> sumBalanceAfterTx(
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
