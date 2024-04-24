//package org.cardanofoundation.job.repository.ledgersync;
//
//import java.sql.Timestamp;
//import java.util.Collection;
//import java.util.List;
//import java.util.Set;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import org.cardanofoundation.explorer.common.entity.enumeration.ScriptType;
//import org.cardanofoundation.explorer.common.entity.ledgersync.MultiAsset;
//import org.cardanofoundation.job.projection.ScriptNumberTokenProjection;
//
//@Repository
//public interface MultiAssetRepository extends JpaRepository<MultiAsset, Long> {
//  @Query(
//      "select distinct multiAsset "
//          + " from MultiAsset multiAsset join AddressToken addressToken"
//          + " on multiAsset.id = addressToken.multiAssetId"
//          + " join Tx tx on tx.id = addressToken.txId"
//          + " join Block block on block.id = tx.blockId"
//          + " where block.blockNo > :fromBlockNo and block.blockNo <= :toBlockNo ")
//  List<MultiAsset> getTokensInTransactionInBlockRange(
//      @Param("fromBlockNo") Long fromBlockNo, @Param("toBlockNo") Long toBlockNo);
//
//  @Query(
//      "select multiAsset from MultiAsset multiAsset where multiAsset.time >= :time"
//          + " and multiAsset.txCount = 0")
//  List<MultiAsset> getTokensWithZeroTxCountAndAfterTime(@Param("time") Timestamp afterTime);
//
//  @Query(
//      "select distinct multiAsset "
//          + " from MultiAsset multiAsset join AddressToken addressToken"
//          + " on multiAsset.id = addressToken.multiAssetId"
//          + " join Tx tx on tx.id = addressToken.txId"
//          + " join Block block on block.id = tx.blockId"
//          + " where block.time >= :fromTime and block.time <= :toTime")
//  List<MultiAsset> getTokensInTransactionInTimeRange(
//      @Param("fromTime") Timestamp fromTime, @Param("toTime") Timestamp toTime);
//
//  @Query("SELECT max(multiAsset.id) FROM MultiAsset multiAsset")
//  Long getCurrentMaxIdent();
//
//  @Query(
//      "SELECT count(multiAsset) as numberOfTokens, multiAsset.policy as scriptHash"
//          + " FROM MultiAsset multiAsset"
//          + " WHERE multiAsset.policy IN :policyIds"
//          + " GROUP BY multiAsset.policy")
//  List<ScriptNumberTokenProjection> countByPolicyIn(
//      @Param("policyIds") Collection<String> policyIds);
//
//  @Query(
//      "SELECT multiAsset.policy as scriptHash"
//          + " FROM MultiAsset multiAsset"
//          + " INNER JOIN Script script ON script.hash = multiAsset.policy AND script.type IN :types"
//          + " INNER JOIN AddressToken addressToken ON addressToken.multiAssetId = multiAsset.id"
//          + " WHERE addressToken.txId BETWEEN :fromTxId AND :toTxId")
//  Set<String> findPolicyByTxIn(
//      @Param("fromTxId") Long fromTxId,
//      @Param("toTxId") Long toTxId,
//      @Param("types") Collection<ScriptType> types);
//}
