package com.sotatek.cardano.job.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sotatek.cardano.common.entity.Block;
import com.sotatek.cardano.common.entity.Tx;
import com.sotatek.cardano.job.projection.TxRangeProjection;

@Repository
public interface TxRepository extends JpaRepository<Tx, Long> {

  List<Tx> findAllByBlockIn(Collection<Block> blocks);

  @Query(
      "SELECT MAX(tx.id) AS maxTxId, MIN(tx.id) AS minTxId "
          + "FROM Tx tx "
          + "WHERE tx.block.id >= :minBlockId AND tx.block.id <= :maxBlockId")
  TxRangeProjection findBlockIdsRangeInEpoch(Long minBlockId, Long maxBlockId);
}
