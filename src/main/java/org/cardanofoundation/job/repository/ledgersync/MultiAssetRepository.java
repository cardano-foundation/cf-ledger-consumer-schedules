package org.cardanofoundation.job.repository.ledgersync;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.common.entity.enumeration.ScriptType;
import org.cardanofoundation.explorer.common.entity.ledgersync.MultiAsset;
import org.cardanofoundation.job.projection.ScriptNumberTokenProjection;
import org.cardanofoundation.job.projection.TokenUnitProjection;

@Repository
public interface MultiAssetRepository extends JpaRepository<MultiAsset, Long> {

  @Query(
      "SELECT multiAsset.id AS ident, multiAsset.unit AS unit FROM MultiAsset multiAsset "
          + "WHERE multiAsset.id >= :startIdent AND multiAsset.id <= :endIdent")
  List<TokenUnitProjection> getTokenUnitByIdBetween(
      @Param("startIdent") Long startIdent, @Param("endIdent") Long endIdent);

  @Query(
      "SELECT multiAsset.id AS ident, multiAsset.unit AS unit FROM MultiAsset multiAsset WHERE multiAsset.unit IN :units")
  List<TokenUnitProjection> getTokenUnitByUnitIn(@Param("units") List<String> units);

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

  @Query("SELECT multiAsset.unit AS unit FROM MultiAsset multiAsset")
  Slice<String> getTokenUnitSlice(Pageable pageable);
}
