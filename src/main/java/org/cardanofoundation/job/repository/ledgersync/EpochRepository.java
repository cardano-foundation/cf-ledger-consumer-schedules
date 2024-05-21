package org.cardanofoundation.job.repository.ledgersync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.common.entity.ledgersync.Epoch;

public interface EpochRepository extends JpaRepository<Epoch, Long> {

  @Query("SELECT MAX(epoch.no) FROM Epoch epoch")
  Integer findMaxEpochNo();
}
