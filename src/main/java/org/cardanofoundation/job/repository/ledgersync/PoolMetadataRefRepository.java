package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.consumercommon.entity.PoolHash;
import org.cardanofoundation.explorer.consumercommon.entity.PoolMetadataRef;

@Repository
public interface PoolMetadataRefRepository extends JpaRepository<PoolMetadataRef, Long> {

  Optional<PoolMetadataRef> findPoolMetadataRefByPoolHashAndUrlAndHash(
      PoolHash poolHash, String url, String hash);

  Optional<PoolMetadataRef> findById(Long id);

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  List<PoolMetadataRef> findByIdIn(List<Long> poolMetadataIds);
}
