package org.cardanofoundation.job.repository.ledgersync;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.consumercommon.entity.Block;
import org.cardanofoundation.job.projection.PoolCountProjection;

public interface BlockRepository extends JpaRepository<Block, Long> {

  @Query("select max(b.time) from Block b")
  Optional<Timestamp> getMaxTime();

  @Query("select b from Block b where b.blockNo = "
      + "(select max(blockNo) from Block)")
  Optional<Block> findLatestBlock();

  @Query(value = "SELECT ph.id AS poolId, count(bk.id) AS countValue "
      + "FROM PoolHash ph "
      + "JOIN SlotLeader sl ON sl.poolHash.id = ph.id "
      + "JOIN Block bk ON bk.slotLeader.id = sl.id "
      + "GROUP BY ph.id")
  List<PoolCountProjection> getCountBlockByPools();

  @Query(value = "SELECT ph.id AS poolId, count(bk.id) AS countValue "
      + "FROM PoolHash ph "
      + "JOIN SlotLeader sl ON sl.poolHash.id = ph.id "
      + "JOIN Block bk ON bk.slotLeader.id = sl.id "
      + "WHERE bk.epochNo = (SELECT MAX(e.no) FROM Epoch e)"
      + "GROUP BY ph.id")
  List<PoolCountProjection> getAllCountBlockInCurrentEpoch();
}