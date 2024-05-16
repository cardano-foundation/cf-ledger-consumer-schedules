package org.cardanofoundation.job.repository.ledgersync;

import java.util.Collection;
import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.compositeKey.AddressBalanceId;
import org.cardanofoundation.explorer.common.entity.ledgersync.LatestTokenBalance;
import org.cardanofoundation.job.model.TokenNumberHolders;
import org.cardanofoundation.job.projection.ScriptNumberHolderProjection;

public interface LatestTokenBalanceRepository
    extends JpaRepository<LatestTokenBalance, AddressBalanceId> {

  @Query(
      """
          SELECT multiAsset.policy as scriptHash, COALESCE(COUNT(latestTokenBalance), 0) as numberOfHolders
          FROM MultiAsset multiAsset
          LEFT JOIN LatestTokenBalance latestTokenBalance ON multiAsset.unit = latestTokenBalance.unit
          WHERE multiAsset.policy IN :policies
          GROUP BY multiAsset.policy
      """)
  List<ScriptNumberHolderProjection> countHolderByPolicyIn(
      @Param("policies") Collection<String> policies);

  @Query(
      """
          SELECT new org.cardanofoundation.job.model.TokenNumberHolders(multiAsset.id, COUNT(latestTokenBalance))
          FROM MultiAsset multiAsset
          LEFT JOIN LatestTokenBalance latestTokenBalance ON multiAsset.unit = latestTokenBalance.unit
          WHERE multiAsset.id IN :multiAssetIds
          AND latestTokenBalance.quantity > 0
          GROUP BY multiAsset.id
      """)
  List<TokenNumberHolders> countHoldersByMultiAssetIdIn(
      @Param("multiAssetIds") List<Long> multiAssetIds);

  @Query(
      """
          SELECT new org.cardanofoundation.job.model.TokenNumberHolders(multiAsset.id, COUNT(latestTokenBalance))
          FROM MultiAsset multiAsset
          LEFT JOIN LatestTokenBalance latestTokenBalance ON multiAsset.unit = latestTokenBalance.unit
          WHERE multiAsset.id >= :startIdent AND multiAsset.id <= :endIdent
          AND latestTokenBalance.quantity > 0
          GROUP BY multiAsset.id
      """)
  List<TokenNumberHolders> countHoldersByMultiAssetIdInRange(
      @Param("startIdent") Long startIdent, @Param("endIdent") Long endIdent);

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY latest_token_balance", nativeQuery = true)
  void refreshMaterializedView();
}
