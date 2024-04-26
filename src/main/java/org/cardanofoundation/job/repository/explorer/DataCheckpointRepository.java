package org.cardanofoundation.job.repository.explorer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.common.entity.enumeration.DataCheckpointType;
import org.cardanofoundation.explorer.common.entity.explorer.DataCheckpoint;

public interface DataCheckpointRepository extends JpaRepository<DataCheckpoint, Long> {

  Optional<DataCheckpoint> findFirstByType(DataCheckpointType type);
}
