package org.cardanofoundation.job.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.job.projection.PoolOfflineHashProjection;
import org.cardanofoundation.explorer.consumercommon.entity.PoolOfflineData;

public interface PoolOfflineDataRepository extends JpaRepository<PoolOfflineData, Long> {
  @Query(
      "SELECT pod.pool.id as poolId, pod.poolMetadataRef.id as poolRefId,pod.hash as hash "
          + "FROM PoolOfflineData pod "
          + "WHERE pod.pool.id IN :ids "
          + "ORDER BY pod.pool.id ASC, "
          + "pod.poolMetadataRef.id ASC")
  Set<PoolOfflineHashProjection> findPoolOfflineDataHashByPoolIds(@Param("ids") List<Long> ids);

  Optional<PoolOfflineData> findByPoolIdAndAndPmrId(Long poolId, Long pmrId);
}