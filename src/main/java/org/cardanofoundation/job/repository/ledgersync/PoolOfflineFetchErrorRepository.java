package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.PoolHash;
import org.cardanofoundation.explorer.consumercommon.entity.PoolMetadataRef;
import org.cardanofoundation.explorer.consumercommon.entity.PoolOfflineFetchError;

@Repository
public interface PoolOfflineFetchErrorRepository
    extends JpaRepository<PoolOfflineFetchError, Long> {

  @Query(
      "SELECT poe FROM PoolOfflineFetchError poe WHERE poe.poolMetadataRef.id IN :poolMetadataIds ")
  List<PoolOfflineFetchError> findPoolOfflineFetchErrorByPoolMetadataRefIn(
      @Param("poolMetadataIds") List<Long> poolMetadataId);

  PoolOfflineFetchError findByPoolHashAndPoolMetadataRef(PoolHash poolHash, PoolMetadataRef poolMetadataRef);
}
