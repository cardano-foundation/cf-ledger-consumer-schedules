package org.cardanofoundation.job.repository.ledgersync;

import java.sql.Timestamp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.StakeAddress;
import org.cardanofoundation.explorer.consumercommon.entity.StakeRegistration;
import org.cardanofoundation.job.projection.StakeHistoryProjection;

@Repository
public interface StakeRegistrationRepository extends JpaRepository<StakeRegistration, Long> {

  @Query(
      value =
          "SELECT tx.hash as txHash, b.time as time,"
              + " b.epochSlotNo as epochSlotNo, b.blockNo as blockNo, b.epochNo as epochNo,"
              + " 'Registered' AS action, tx.blockIndex as blockIndex, tx.fee as fee, tx.deposit as deposit"
              + " FROM StakeRegistration sr"
              + " JOIN Tx tx ON tx.id = sr.tx.id"
              + " JOIN Block b ON b.id = tx.blockId"
              + " WHERE sr.addr = :stakeKey"
              + " AND (b.time >= :fromTime ) "
              + " AND (b.time <= :toTime)"
              + " AND ( :txHash IS NULL OR tx.hash = :txHash)")
  Page<StakeHistoryProjection> getStakeRegistrationsByAddress(
      @Param("stakeKey") StakeAddress stakeKey,
      @Param("txHash") String txHash,
      @Param("fromTime") Timestamp fromTime,
      @Param("toTime") Timestamp toTime,
      Pageable pageable);
}
