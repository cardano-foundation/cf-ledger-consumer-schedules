package org.cardanofoundation.job.repository.ledgersync;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.common.entity.ledgersync.DRepRegistrationEntity;
import org.cardanofoundation.explorer.common.entity.ledgersync.compositeKey.DRepRegistrationId;

public interface DRepRegistrationRepository
    extends JpaRepository<DRepRegistrationEntity, DRepRegistrationId> {

  Slice<DRepRegistrationEntity> findAllBySlotGreaterThan(Long slot, Pageable pageable);
}
