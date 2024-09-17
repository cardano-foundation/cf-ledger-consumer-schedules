package org.cardanofoundation.job.repository.explorer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.enumeration.DataCheckpointType;
import org.cardanofoundation.explorer.common.entity.explorer.DataCheckpoint;

public interface DataCheckpointRepository extends JpaRepository<DataCheckpoint, Long> {

  @Query("SELECT d FROM DataCheckpoint d WHERE d.type = :type")
  Optional<DataCheckpoint> findFirstByType(@Param("type") DataCheckpointType type);
}
