package org.cardanofoundation.job.repository.ledgersync;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.common.entity.compositeKey.OffChainVoteGovActionDataId;
import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteGovActionData;

public interface OffChainVoteGovActionRepository
    extends JpaRepository<OffChainVoteGovActionData, OffChainVoteGovActionDataId> {}
