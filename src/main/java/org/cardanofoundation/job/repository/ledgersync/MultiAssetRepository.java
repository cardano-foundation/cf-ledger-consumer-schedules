package org.cardanofoundation.job.repository.ledgersync;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.common.entity.enumeration.ScriptType;
import org.cardanofoundation.explorer.common.entity.ledgersync.MultiAsset;
import org.cardanofoundation.job.projection.ScriptNumberTokenProjection;

@Repository
public interface MultiAssetRepository extends JpaRepository<MultiAsset, Long> {
  @Query(
      "SELECT count(multiAsset) as numberOfTokens, multiAsset.policy as scriptHash"
          + " FROM MultiAsset multiAsset"
          + " WHERE multiAsset.policy IN :policyIds"
          + " GROUP BY multiAsset.policy")
  List<ScriptNumberTokenProjection> countByPolicyIn(
      @Param("policyIds") Collection<String> policyIds);

  @Query(
      value =
          """
          SELECT multiAsset.policy as scriptHash
                FROM MultiAsset multiAsset
                INNER JOIN Script script ON script.hash = multiAsset.policy AND script.type IN :types
                INNER JOIN AddressTxAmount addressTxAmount ON addressTxAmount.unit = multiAsset.unit
                INNER JOIN Tx tx ON tx.hash = addressTxAmount.txHash
                WHERE tx.id BETWEEN :fromTxId AND :toTxId
      """)
  Set<String> findPolicyByTxIn(
      @Param("fromTxId") Long fromTxId,
      @Param("toTxId") Long toTxId,
      @Param("types") Collection<ScriptType> types);

  @Query("SELECT max(multiAsset.id) FROM MultiAsset multiAsset")
  Long getCurrentMaxIdent();

  @Query(
      "select distinct multiAsset "
          + " from MultiAsset multiAsset "
          + " join AddressTxAmount addressTxAmount on multiAsset.unit = addressTxAmount.unit"
          + " join Tx tx on tx.hash = addressTxAmount.txHash"
          + " join Block block on block.id = tx.blockId"
          + " where block.blockNo > :fromBlockNo and block.blockNo <= :toBlockNo ")
  List<MultiAsset> getTokensInTransactionInBlockRange(
      @Param("fromBlockNo") Long fromBlockNo, @Param("toBlockNo") Long toBlockNo);

  @Query(
      "select distinct multiAsset "
          + " from MultiAsset multiAsset "
          + " join AddressTxAmount addressTxAmount on multiAsset.unit = addressTxAmount.unit"
          + " join Tx tx on tx.hash = addressTxAmount.txHash"
          + " join Block block on block.id = tx.blockId"
          + " where block.time >= :fromTime and block.time <= :toTime")
  List<MultiAsset> getTokensInTransactionInTimeRange(
      @Param("fromTime") Timestamp fromTime, @Param("toTime") Timestamp toTime);
}
