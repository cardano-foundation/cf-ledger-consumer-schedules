package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

  @Query(
      value =
          "SELECT ph.id AS poolId, pmr.url as url, pmr.id as metadataId FROM PoolHash ph "
              + "JOIN PoolUpdate pu ON (ph = pu.poolHash AND pu.id = (SELECT max(pu2.id) from PoolUpdate pu2 WHERE pu2.activeEpochNo <= (SELECT MAX(e.no) FROM Epoch e) AND pu2.poolHash = ph)) "
              + "JOIN PoolMetadataRef pmr ON (pmr = pu.meta) "
              + "LEFT JOIN PoolOfflineFetchError pofe ON (pofe.poolMetadataRef = pmr AND pofe.poolHash = ph) "
              + "WHERE (pofe.retryCount < 5 OR pofe.retryCount IS NULL)"
              + "AND NOT EXISTS(SELECT pod.id FROM PoolOfflineData pod WHERE pod.pool = ph AND pod.poolMetadataRef = pmr) "
              + "ORDER BY ph.id ASC"
  )
  List<PoolHashUrlProjection> findPoolHashAndUrl(Pageable pageable);

  @Query(value =
      "SELECT pu.id AS poolUpdateId, pu.pledge AS pledge, pu.margin AS margin, pu.vrfKeyHash AS vrfKey, pu.fixedCost AS cost, tx.hash AS txHash, bk.time AS time, ep.poolDeposit AS deposit, tx.fee AS fee, sa.view AS rewardAccount "
          + "FROM PoolUpdate pu "
          + "JOIN Tx tx ON pu.registeredTx.id = tx.id "
          + "JOIN Block bk ON tx.block.id  = bk.id "
          + "JOIN EpochParam ep ON ep.epochNo = bk.epochNo "
          + "JOIN StakeAddress sa ON pu.rewardAddr.id = sa.id "
          + "WHERE pu.id IN :poolCertificateIds ")
  Page<PoolRegistrationProjection> getPoolRegistrationByPool(@Param("poolCertificateIds") Set<Long> poolCertificateIds,
                                                             Pageable pageable);

  Optional<PoolHash> findById(Long id);

  @Query(
      value =
          "SELECT ph.id AS id, pod.poolName AS poolName, ph.hashRaw AS poolId, ph.view AS poolView "
              + "FROM PoolHash ph "
              + "LEFT JOIN PoolOfflineData pod ON ph.id  = pod.pool.id AND pod.id = (SELECT max(pod2.id) FROM PoolOfflineData pod2 WHERE ph.id = pod2.pool.id ) "
              + "WHERE ph.view = :poolView "
              + "OR ph.hashRaw = :poolView")
  PoolInfoProjection getPoolInfo(@Param("poolView") String poolView);

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  List<PoolHash> findByIdIn(List<Long> poolId);
}
