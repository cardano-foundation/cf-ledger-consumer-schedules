package org.cardanofoundation.job.repository.ledgersync;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.common.entity.compositeKey.OffChainVoteFetchErrorId;
import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteFetchError;

public interface OffChainVoteFetchErrorRepository
    extends JpaRepository<OffChainVoteFetchError, OffChainVoteFetchErrorId> {}
