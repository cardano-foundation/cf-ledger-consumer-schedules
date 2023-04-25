package com.sotatek.cardano.job.repository;

import com.sotatek.cardano.common.entity.PoolHash;
import com.sotatek.cardano.common.entity.PoolMetadataRef;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PoolMetadataRefRepository extends JpaRepository<PoolMetadataRef, Long> {

  Optional<PoolMetadataRef> findPoolMetadataRefByPoolHashAndUrlAndHash(
      PoolHash poolHash, String url, String hash);

  Optional<PoolMetadataRef> findById(Long id);
}
