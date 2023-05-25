package org.cardanofoundation.job.repository;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.Delegation;
import org.cardanofoundation.explorer.consumercommon.entity.StakeAddress;
import org.cardanofoundation.job.projection.StakeDelegationProjection;

@Repository
public interface DelegationRepository extends JpaRepository<Delegation, Long> {

  @Query("SELECT delegation.tx.id"
      + " FROM Delegation delegation"
      + " WHERE delegation.address = :stakeKey AND delegation.tx.id IN :txIds")
  List<Long> findDelegationByAddressAndTxIn(@Param("stakeKey") StakeAddress stakeKey,
                                            @Param("txIds") Collection<Long> txIds);

  @Query("SELECT tx.hash as txHash, block.time as time, block.epochSlotNo as epochSlotNo,"
      + " block.blockNo as blockNo, block.epochNo as epochNo, tx.fee as fee, tx.outSum as outSum"
      + " FROM Delegation delegation"
      + " INNER JOIN Tx tx ON delegation.tx = tx"
      + " INNER JOIN Block block ON tx.block = block"
      + " WHERE delegation.address = :stakeKey"
      + " AND (block.time >= :fromTime ) "
      + " AND (block.time <= :toTime)"
      + " AND ( :txHash IS NULL OR tx.hash = :txHash)")
  Page<StakeDelegationProjection> findDelegationByAddress(@Param("stakeKey") StakeAddress stakeKey,
                                                          @Param("txHash") String txHash,
                                                          @Param("fromTime") Timestamp fromTime,
                                                          @Param("toTime") Timestamp toTime,
                                                          Pageable pageable);
}
