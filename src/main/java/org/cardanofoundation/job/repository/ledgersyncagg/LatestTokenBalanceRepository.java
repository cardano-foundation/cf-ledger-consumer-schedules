package org.cardanofoundation.job.repository.ledgersyncagg;

import java.util.Collection;
import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.compositeKey.AddressBalanceId;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.LatestTokenBalance;
import org.cardanofoundation.job.model.TokenNumberHolders;
import org.cardanofoundation.job.projection.ScriptNumberHolderProjection;

public interface LatestTokenBalanceRepository
    extends JpaRepository<LatestTokenBalance, AddressBalanceId> {

  @Query(
      value =
          """
          select max(ltb.block_time) from latest_token_balance ltb
          where ltb.block_time != (select max(ltb2.block_time) from latest_token_balance ltb2)
      """,
      nativeQuery = true)
  Long getTheSecondLastBlockTime();

  @Query(
      value =
          """
                  select max(ltb.slot) from latest_token_balance ltb
                  where ltb.slot != (select max(ltb2.slot) from latest_token_balance ltb2)
              """,
      nativeQuery = true)
  Long getTheSecondLastSlot();

  @Query(
      """
          SELECT latestTokenBalance.policy as scriptHash, COALESCE(COUNT(latestTokenBalance), 0) as numberOfHolders
          FROM LatestTokenBalance latestTokenBalance
          WHERE latestTokenBalance.policy IN :policies
          AND latestTokenBalance.quantity > 0
          GROUP BY latestTokenBalance.policy
      """)
  List<ScriptNumberHolderProjection> countHolderByPolicyIn(
      @Param("policies") Collection<String> policies);

  @Query(
      """
          SELECT new org.cardanofoundation.job.model.TokenNumberHolders
          (latestTokenBalance.unit, COUNT(DISTINCT(CASE WHEN latestTokenBalance.stakeAddress IS NULL
           THEN latestTokenBalance.address ELSE latestTokenBalance.stakeAddress END)))
          FROM LatestTokenBalance latestTokenBalance
          WHERE latestTokenBalance.unit in :units
          AND latestTokenBalance.quantity > 0
          GROUP BY latestTokenBalance.unit
      """)
  List<TokenNumberHolders> countHoldersByMultiAssetIdInRange(@Param("units") List<String> units);

  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY latest_token_balance", nativeQuery = true)
  void refreshMaterializedView();
}
