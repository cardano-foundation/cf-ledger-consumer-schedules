package org.cardanofoundation.job.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.PoolHash;
import org.cardanofoundation.explorer.consumercommon.entity.PoolMetadataRef;

@Repository
public interface PoolMetadataRefRepository extends JpaRepository<PoolMetadataRef, Long> {

  Optional<PoolMetadataRef> findPoolMetadataRefByPoolHashAndUrlAndHash(
      PoolHash poolHash, String url, String hash);

  Optional<PoolMetadataRef> findById(Long id);

  List<PoolMetadataRef> findByIdIn(List<Long> poolMetadataIds);
}
