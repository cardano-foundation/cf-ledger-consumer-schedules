package org.cardanofoundation.job.repository.ledgersync;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.common.entity.ledgersync.MultiAsset;
import org.cardanofoundation.job.projection.ScriptNumberTokenProjection;
import org.cardanofoundation.job.projection.TokenUnitProjection;

@Repository
public interface MultiAssetRepository extends JpaRepository<MultiAsset, Long> {

  @Query(
      "SELECT multiAsset.id AS ident, multiAsset.unit AS unit FROM MultiAsset multiAsset WHERE multiAsset.id IN :ids")
  List<TokenUnitProjection> getTokenUnitByIdIn(@Param("ids") List<Long> ids);

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

  @Query("SELECT max(multiAsset.id) FROM MultiAsset multiAsset")
  Long getCurrentMaxIdent();

  @Query("SELECT multiAsset.unit AS unit FROM MultiAsset multiAsset")
  Slice<String> getTokenUnitSlice(Pageable pageable);
}
