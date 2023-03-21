package com.sotatek.cardano.job.repository;

import com.sotatek.cardano.job.projection.PoolHashUrlProjection;
import com.sotatek.cardano.common.entity.PoolHash;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PoolHashRepository extends JpaRepository<PoolHash, Long> {

  //@Query("SELECT PH.hashRaw FROM PoolHash PH WHERE ")
  Optional<PoolHash> findPoolHashByHashRaw(String hashBytes);

  Optional<PoolHash> findPoolHashByView(String view);

  @Query("SELECT ph.id AS poolId, pmr.url as url, pmr.id as metadataId "
      + "FROM PoolHash ph "
      + "JOIN  PoolMetadataRef pmr ON pmr.poolHash.id = ph.id "
      + "LEFT JOIN PoolOfflineFetchError pofe ON pofe.poolHash.id = ph.id AND pofe.poolMetadataRef.id = pmr.id "
      + "ORDER BY ph.id ASC, pmr.id ASC")
  List<PoolHashUrlProjection> findPoolHashAndUrl(Pageable pageable);

  Optional<PoolHash> findById(Long id);
}
