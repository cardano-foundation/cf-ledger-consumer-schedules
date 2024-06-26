package org.cardanofoundation.job.repository.explorer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.common.entity.explorer.TokenInfoCheckpoint;

@Repository
public interface TokenInfoCheckpointRepository extends JpaRepository<TokenInfoCheckpoint, Long> {
  @Query(
      "select t from TokenInfoCheckpoint t where t.id = "
          + "(select max(id) from TokenInfoCheckpoint)")
  Optional<TokenInfoCheckpoint> findLatestTokenInfoCheckpoint();
}
