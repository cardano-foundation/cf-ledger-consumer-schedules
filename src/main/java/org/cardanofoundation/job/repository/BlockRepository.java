package org.cardanofoundation.job.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import org.cardanofoundation.job.projection.BlockRangeProjection;
import org.cardanofoundation.explorer.consumercommon.entity.Block;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {

  Optional<Block> findBlockByHash(String hash);

  Optional<Block> findBlockByBlockNo(long number);

  boolean existsBlockByHash(String hash);

  List<Block> findAllByBlockNoGreaterThanOrderByBlockNoDesc(Long blockNo);

  @Query("SELECT MAX(block.blockNo) FROM Block block")
  Optional<Long> getBlockHeight();

  @Query(
      "SELECT MAX(b.id) AS maxBlockId, MIN(b.id) AS minBlockId "
          + "FROM Block b "
          + "WHERE b.epochNo = :epochNo")
  BlockRangeProjection findBlockIdsRangeInEpoch(Integer epochNo);
}
