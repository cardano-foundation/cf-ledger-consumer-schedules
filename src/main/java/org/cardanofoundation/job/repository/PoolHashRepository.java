package org.cardanofoundation.job.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.job.projection.PoolHashUrlProjection;
import org.cardanofoundation.explorer.consumercommon.entity.PoolHash;
import org.cardanofoundation.job.projection.PoolInfoProjection;
import org.cardanofoundation.job.projection.PoolRegistrationProjection;

@Repository
public interface PoolHashRepository extends JpaRepository<PoolHash, Long> {

  @Query(
      "SELECT ph.id AS poolId, pmr.url as url, pmr.id as metadataId "
          + "FROM PoolHash ph "
          + "JOIN  PoolMetadataRef pmr ON pmr.poolHash.id = ph.id "
          + "LEFT JOIN PoolOfflineFetchError pofe ON pofe.poolHash.id = ph.id AND pofe.poolMetadataRef.id = pmr.id "
          + "ORDER BY ph.id ASC, pmr.id ASC")
  List<PoolHashUrlProjection> findPoolHashAndUrl(Pageable pageable);

  @Query(value =
      "SELECT pu.id AS poolUpdateId, pu.pledge AS pledge, pu.margin AS margin, pu.vrfKeyHash AS vrfKey, pu.fixedCost AS cost, tx.hash AS txHash, bk.time AS time, tx.deposit AS deposit, tx.fee AS fee, sa.view AS rewardAccount "
          + "FROM PoolHash ph "
          + "JOIN PoolUpdate pu ON ph.id = pu.poolHash.id "
          + "JOIN Tx tx ON pu.registeredTx.id = tx.id AND tx.deposit IS NOT NULL AND tx.deposit > 0 "
          + "JOIN Block bk ON tx.block.id  = bk.id "
          + "JOIN StakeAddress sa ON pu.rewardAddr.id = sa.id "
          + "WHERE ph.view = :poolView")
  Page<PoolRegistrationProjection> getPoolRegistrationByPool(@Param("poolView") String poolView,
                                                             Pageable pageable);

  Optional<PoolHash> findById(Long id);

  @Query(value = "SELECT ph.id AS id, pod.poolName AS poolName, ph.hashRaw AS poolId, ph.view AS poolView "
      + "FROM PoolHash ph "
      + "LEFT JOIN PoolOfflineData pod ON ph.id  = pod.pool.id AND pod.id = (SELECT max(pod2.id) FROM PoolOfflineData pod2 WHERE ph.id = pod2.pool.id ) "
      + "WHERE ph.view = :poolView")
  PoolInfoProjection getPoolInfo(@Param("poolView") String poolView);

  List<PoolHash> findByHashRawIn(Collection<String> hashes);
}
