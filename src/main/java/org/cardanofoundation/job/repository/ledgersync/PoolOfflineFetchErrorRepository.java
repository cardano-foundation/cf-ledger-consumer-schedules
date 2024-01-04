package org.cardanofoundation.job.repository.ledgersync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.PoolHash;
import org.cardanofoundation.explorer.consumercommon.entity.PoolMetadataRef;
import org.cardanofoundation.explorer.consumercommon.entity.PoolOfflineFetchError;

@Repository
public interface PoolOfflineFetchErrorRepository extends
    JpaRepository<PoolOfflineFetchError, Long> {

  PoolOfflineFetchError findByPoolHashAndPoolMetadataRef(PoolHash poolHash,
                                                         PoolMetadataRef poolMetadataRef);
}
