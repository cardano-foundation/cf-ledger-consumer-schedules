package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.ledgersync.PoolUpdate;
import org.cardanofoundation.job.projection.PoolCertificateProjection;
import org.cardanofoundation.job.projection.PoolUpdateDetailProjection;
import org.cardanofoundation.job.projection.PoolUpdateTxProjection;

public interface PoolUpdateRepository extends JpaRepository<PoolUpdate, Long> {
  @Query(
      value =
          "SELECT sa.view FROM PoolUpdate pu "
              + "JOIN PoolOwner po ON pu.id = po.poolUpdate.id "
              + "JOIN StakeAddress sa ON po.stakeAddress.id = sa.id "
              + "WHERE pu.id  = :id ")
  List<String> findOwnerAccountByPoolUpdate(@Param("id") Long id);

  @Query(
      value =
          "SELECT pu.id AS poolUpdateId, ph.id AS hashId, ph.hashRaw AS poolId , ph.view AS poolView, pod.poolName AS poolName, pu.pledge AS pledge, pu.margin AS margin, pu.vrfKeyHash AS vrfKey, pu.fixedCost  AS cost, tx.hash AS txHash, bk.time AS time, tx.fee AS fee, sa.view AS rewardAccount, tx.deposit AS deposit "
              + "FROM PoolHash ph "
              + "LEFT JOIN PoolOfflineData pod ON ph.id = pod.pool.id AND pod.id = (SELECT max(pod2.id) FROM PoolOfflineData pod2 WHERE ph.id = pod2.pool.id) "
              + "JOIN PoolUpdate pu ON ph.id = pu.poolHash.id "
              + "JOIN Tx tx ON pu.registeredTx.id = tx.id "
              + "JOIN Block bk ON tx.block.id  = bk.id "
              + "JOIN StakeAddress sa ON pu.rewardAddr.id  = sa.id "
              + "WHERE pu.id IN :poolCertificateIds ")
  Page<PoolUpdateDetailProjection> findPoolUpdateByPool(
      @Param("poolCertificateIds") Set<Long> poolCertificateIds, Pageable pageable);

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

  @Query(
      "SELECT  new org.cardanofoundation.job.projection.PoolUpdateTxProjection(poolUpdate.registeredTxId, poolUpdate.poolHashId, MAX(poolUpdate.certIndex)) "
          + "FROM PoolUpdate poolUpdate "
          + "WHERE (poolUpdate.registeredTxId, poolUpdate.poolHashId) IN "
          + "(SELECT MAX(pu.registeredTxId), pu.poolHashId FROM PoolUpdate pu GROUP BY pu.poolHashId)"
          + "GROUP BY poolUpdate.registeredTxId, poolUpdate.poolHashId")
  List<PoolUpdateTxProjection> findLastPoolCertificate();

  @Query(
      value =
          "SELECT tx.id as txId, tx.hash as txHash, b.epochNo as txEpochNo,"
              + "pu.activeEpochNo as certEpochNo, pu.certIndex as certIndex, pu.id as poolUpdateId, "
              + "b.time as blockTime, b.blockNo as blockNo, b.epochSlotNo as epochSlotNo, b.slotNo as slotNo "
              + "FROM PoolUpdate pu "
              + "JOIN Tx tx on pu.registeredTx = tx "
              + "JOIN Block b on tx.block = b "
              + "WHERE pu.poolHash.view = :poolViewOrHash "
              + "OR pu.poolHash.hashRaw = :poolViewOrHash ")
  List<PoolCertificateProjection> getPoolUpdateByPoolViewOrHash(
      @Param("poolViewOrHash") String poolViewOrHash);

  @Query(
      value =
          "SELECT bk.slotNo FROM PoolUpdate pu "
              + "JOIN Tx t ON pu.registeredTx.id = t.id "
              + "JOIN Block bk ON t.block.id = bk.id "
              + "WHERE pu.id = (SELECT min(pu2.id) FROM PoolUpdate pu2 WHERE pu2.poolHash.id = :poolId) "
              + "AND pu.poolHash.id = :poolId ")
  Long getCreatedSlotOfPool(@Param("poolId") Long poolId);
}
