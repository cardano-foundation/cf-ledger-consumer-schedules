package org.cardanofoundation.job.repository.ledgersync;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.common.entity.ledgersync.GovActionProposalInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.compositeKey.GovActionProposalId;

public interface GovActionProposalInfoRepository
    extends JpaRepository<GovActionProposalInfo, GovActionProposalId> {}
