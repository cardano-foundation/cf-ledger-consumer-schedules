package org.cardanofoundation.job.repository.ledgersync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.common.entity.ledgersync.DRepRegistrationEntity;
import org.cardanofoundation.explorer.common.entity.ledgersync.compositeKey.DRepRegistrationId;

@Repository
public interface DRepRegistrationRepository
    extends JpaRepository<DRepRegistrationEntity, DRepRegistrationId> {}
