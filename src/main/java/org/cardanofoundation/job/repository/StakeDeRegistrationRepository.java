package org.cardanofoundation.job.repository;

import java.sql.Timestamp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.StakeAddress;
import org.cardanofoundation.explorer.consumercommon.entity.StakeDeregistration;
import org.cardanofoundation.job.projection.StakeHistoryProjection;

@Repository
public interface StakeDeRegistrationRepository extends JpaRepository<StakeDeregistration, Long> {

  @Query(
      value =
          "SELECT tx.hash as txHash, b.time as time,"
              + " b.epochSlotNo as epochSlotNo, b.blockNo as blockNo, b.epochNo as epochNo,"
              + " 'De Registered' AS action, tx.blockIndex as blockIndex, tx.fee as fee, tx.deposit as deposit"
              + " FROM StakeDeregistration dr"
              + " JOIN Tx tx ON tx.id = dr.tx.id"
              + " JOIN Block b ON b.id = tx.blockId"
              + " WHERE dr.addr = :stakeKey"
              + " AND (b.time >= :fromTime ) "
              + " AND (b.time <= :toTime)"
              + " AND ( :txHash IS NULL OR tx.hash = :txHash)")
  Page<StakeHistoryProjection> getStakeDeRegistrationsByAddress(
      @Param("stakeKey") StakeAddress stakeKey,
      @Param("txHash") String txHash,
      @Param("fromTime") Timestamp fromTime,
      @Param("toTime") Timestamp toTime,
      Pageable pageable);
}
