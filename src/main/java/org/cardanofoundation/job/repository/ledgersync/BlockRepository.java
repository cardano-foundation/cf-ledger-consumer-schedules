package org.cardanofoundation.job.repository.ledgersync;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.ledgersync.Block;
import org.cardanofoundation.job.projection.PoolCountProjection;

public interface BlockRepository extends JpaRepository<Block, Long> {

  @Query("SELECT b FROM Block b WHERE b.blockNo = " + "(SELECT max(blockNo) FROM Block)")
  Optional<Block> findLatestBlock();

  @Query("SELECT b.time FROM Block b WHERE b.blockNo = :blockNo")
  Timestamp getBlockTimeByBlockNo(@Param("blockNo") Long blockNo);

  @Query(
      value = "SELECT b.block_no FROM block b where extract(epoch FROM b.time) = :time",
      nativeQuery = true)
  Long getBlockNoByExtractEpochTime(@Param("time") Long time);

  @Query(
      value =
          """
                    SELECT ph.id AS poolId, count(bk.id) AS countValue
                    FROM PoolHash ph
                    JOIN SlotLeader sl ON sl.poolHash.id = ph.id
                    JOIN Block bk ON bk.slotLeader.id = sl.id
                    GROUP BY ph.id
                    """)
  List<PoolCountProjection> getCountBlockByPools();

  @Query(
      value =
          """
                    SELECT ph.id AS poolId, count(bk.id) AS countValue
                    FROM PoolHash ph
                    JOIN SlotLeader sl ON sl.poolHash.id = ph.id
                    JOIN Block bk ON bk.slotLeader.id = sl.id
                    WHERE bk.epochNo = (SELECT MAX(e.no) FROM Epoch e)
                    GROUP BY ph.id
                    """)
  List<PoolCountProjection> getAllCountBlockInCurrentEpoch();
}
