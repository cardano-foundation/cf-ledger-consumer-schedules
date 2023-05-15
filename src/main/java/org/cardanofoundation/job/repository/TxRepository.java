package org.cardanofoundation.job.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import org.cardanofoundation.job.projection.TxRangeProjection;
import org.cardanofoundation.explorer.consumercommon.entity.Block;
import org.cardanofoundation.explorer.consumercommon.entity.Tx;

@Repository
public interface TxRepository extends JpaRepository<Tx, Long> {

  List<Tx> findAllByBlockIn(Collection<Block> blocks);

  @Query(
      "SELECT MAX(tx.id) AS maxTxId, MIN(tx.id) AS minTxId "
          + "FROM Tx tx "
          + "WHERE tx.block.id >= :minBlockId AND tx.block.id <= :maxBlockId")
  TxRangeProjection findBlockIdsRangeInEpoch(Long minBlockId, Long maxBlockId);
}
