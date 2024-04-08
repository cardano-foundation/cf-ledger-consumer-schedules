package org.cardanofoundation.job.repository.ledgersync;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.common.entity.compositeKey.GovActionProposalId;
import org.cardanofoundation.explorer.common.entity.ledgersync.GovActionProposalInfo;

public interface GovActionProposalInfoRepository
    extends JpaRepository<GovActionProposalInfo, GovActionProposalId> {}
