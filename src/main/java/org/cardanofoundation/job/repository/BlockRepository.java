package org.cardanofoundation.job.repository;

import java.sql.Timestamp;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.consumercommon.entity.Block;

public interface BlockRepository extends JpaRepository<Block, Long> {

  @Query("select max(b.time) from Block b")
  Optional<Timestamp> getMaxTime();

  @Query("select b from Block b where b.blockNo = "
      + "(select max(blockNo) from Block)")
  Optional<Block> findLatestBlock();
}
