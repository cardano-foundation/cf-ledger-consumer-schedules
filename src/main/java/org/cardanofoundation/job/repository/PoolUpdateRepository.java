package org.cardanofoundation.job.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.consumercommon.entity.PoolUpdate;
import org.cardanofoundation.job.projection.PoolUpdateDetailProjection;
import org.cardanofoundation.job.projection.StakeKeyProjection;

public interface PoolUpdateRepository extends JpaRepository<PoolUpdate, Long> {
  @Query(
      value =
          "SELECT pu.id AS poolUpdateId, sa.view AS view FROM PoolUpdate pu "
              + "JOIN PoolOwner po ON pu.id = po.poolUpdate.id "
              + "JOIN StakeAddress sa ON po.stakeAddress.id = sa.id "
              + "WHERE pu.id IN :poolUpdateIds ")
  List<StakeKeyProjection> findOwnerAccountByPoolUpdate(
      @Param("poolUpdateIds") Set<Long> poolUpdateIds);

  @Query(
      value =
          "SELECT sa.view FROM PoolUpdate pu "
              + "JOIN PoolOwner po ON pu.id = po.poolUpdate.id "
              + "JOIN StakeAddress sa ON po.stakeAddress.id = sa.id "
              + "WHERE pu.id  = :id ")
  List<String> findOwnerAccountByPoolUpdate(@Param("id") Long id);

  @Query(
      value =
          "SELECT pu.id AS poolUpdateId, ph.id AS hashId, ph.hashRaw AS poolId , ph.view AS poolView, pod.poolName AS poolName, pu.pledge AS pledge, pu.margin AS margin, pu.vrfKeyHash AS vrfKey, pu.fixedCost  AS cost, tx.hash AS txHash, bk.time AS time, tx.fee AS fee, sa.view AS rewardAccount "
              + "FROM PoolHash ph "
              + "LEFT JOIN PoolOfflineData pod ON ph.id = pod.pool.id AND pod.id = (SELECT max(pod2.id) FROM PoolOfflineData pod2 WHERE ph.id = pod2.pool.id) "
              + "JOIN PoolUpdate pu ON ph.id = pu.poolHash.id "
              + "JOIN Tx tx ON pu.registeredTx.id = tx.id AND (tx.deposit IS NULL OR tx.deposit = 0) "
              + "JOIN Block bk ON tx.block.id  = bk.id "
              + "JOIN StakeAddress sa ON pu.rewardAddr.id  = sa.id "
              + "WHERE ph.view = :poolView ")
  Page<PoolUpdateDetailProjection> findPoolUpdateByPool(
      @Param("poolView") String poolView, Pageable pageable);

  PoolUpdate findTopByIdLessThanAndPoolHashIdOrderByIdDesc(
      @Param("id") Long id, @Param("poolHashId") Long poolHashId);

  @Query(
      value =
          "SELECT sa.view FROM PoolHash ph "
              + "JOIN PoolUpdate pu ON ph.id = pu.poolHash.id "
              + "JOIN PoolOwner po ON pu.id = po.poolUpdate.id "
              + "JOIN StakeAddress sa ON po.stakeAddress.id = sa.id "
              + "WHERE ph.view  = :poolView "
              + "GROUP BY sa.view")
  List<String> findOwnerAccountByPoolView(@Param("poolView") String poolView);

  @Query(
      value =
          "SELECT sa.view FROM PoolHash ph "
              + "JOIN PoolUpdate pu ON ph.id = pu.poolHash.id "
              + "JOIN StakeAddress sa ON pu.rewardAddr.id = sa.id "
              + "WHERE ph.view = :poolView "
              + "GROUP BY sa.view")
  List<String> findRewardAccountByPoolView(@Param("poolView") String poolView);
}
