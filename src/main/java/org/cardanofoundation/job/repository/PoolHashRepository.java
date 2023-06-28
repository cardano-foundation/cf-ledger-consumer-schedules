package org.cardanofoundation.job.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.consumercommon.entity.PoolHash;
import org.cardanofoundation.job.projection.PoolHashUrlProjection;
import org.cardanofoundation.job.projection.PoolInfoProjection;
import org.cardanofoundation.job.projection.PoolRegistrationProjection;

@Repository
public interface PoolHashRepository extends JpaRepository<PoolHash, Long> {

  /**
   * Get fetched pool without error < 5 times  with raw query
   * <pa>select  ph.hash_raw , pmr.url, pmr.id, ph.id</p>
   *
   * from pool_hash ph
   * join pool_metadata_ref pmr on pmr.pool_id = ph.id
   * left join pool_offline_data pod on pod.pool_id = pmr.pool_id and pod.pmr_id = pmr.id
   * left join pool_offline_fetch_error pofe on pofe.pool_id = pmr.pool_id and pofe.pmr_id = pmr.id
   * where
   * ((pofe.retry_count is null or pofe.retry_count < 5)
   * and pod.pmr_id >= (select max(pod2.pmr_id) from pool_offline_data pod2 where pod2.pool_id = pod.pool_id))
   * or not exists( select pod1.id from pool_offline_data pod1 where pod1.pool_id = ph.id )
   * order  by ph.id, pmr.id
   * @param pageable
   * @return
   */
  @Query(
      "SELECT ph.id AS poolId, pmr.url as url, pmr.id as metadataId "
          + "FROM PoolHash ph "
          + "JOIN  PoolMetadataRef pmr ON pmr.poolHash.id = ph.id "
          + "LEFT JOIN PoolOfflineData pod ON pod.poolMetadataRef.id = pmr.id AND pod.pool.id  =  pmr.poolHash.id "
          + "LEFT JOIN PoolOfflineFetchError pofe ON  pofe.poolMetadataRef.id = pmr.id AND pofe.poolHash.id = pmr.poolHash.id  "
          + "WHERE ((pofe.retryCount < 5 OR pofe.retryCount IS NULL) AND "
          + "pod.pmrId >= (SELECT MAX(pod2.pmrId) FROM PoolOfflineData pod2 WHERE pod2.poolId = pod.poolId)) OR "
          + "NOT EXISTS( SELECT pod3.id FROM PoolOfflineData pod3 WHERE pod3.poolId = ph.id) "
          + "ORDER BY ph.id ASC, pmr.id ASC")
  List<PoolHashUrlProjection> findPoolHashAndUrl(Pageable pageable);

  @Query(
      value =
          "SELECT pu.id AS poolUpdateId, pu.pledge AS pledge, pu.margin AS margin, pu.vrfKeyHash AS vrfKey, pu.fixedCost AS cost, tx.hash AS txHash, bk.time AS time, tx.deposit AS deposit, tx.fee AS fee, sa.view AS rewardAccount "
              + "FROM PoolHash ph "
              + "JOIN PoolUpdate pu ON ph.id = pu.poolHash.id "
              + "JOIN Tx tx ON pu.registeredTx.id = tx.id AND tx.deposit IS NOT NULL AND tx.deposit > 0 "
              + "JOIN Block bk ON tx.block.id  = bk.id "
              + "JOIN StakeAddress sa ON pu.rewardAddr.id = sa.id "
              + "WHERE ph.view = :poolView")
  Page<PoolRegistrationProjection> getPoolRegistrationByPool(
      @Param("poolView") String poolView, Pageable pageable);

  Optional<PoolHash> findById(Long id);

  @Query(
      value =
          "SELECT ph.id AS id, pod.poolName AS poolName, ph.hashRaw AS poolId, ph.view AS poolView "
              + "FROM PoolHash ph "
              + "LEFT JOIN PoolOfflineData pod ON ph.id  = pod.pool.id AND pod.id = (SELECT max(pod2.id) FROM PoolOfflineData pod2 WHERE ph.id = pod2.pool.id ) "
              + "WHERE ph.view = :poolView")
  PoolInfoProjection getPoolInfo(@Param("poolView") String poolView);

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  List<PoolHash> findByIdIn(List<Long> poolId);
}
