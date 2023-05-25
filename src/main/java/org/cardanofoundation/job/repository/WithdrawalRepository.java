package org.cardanofoundation.job.repository;

import java.sql.Timestamp;
import java.util.List;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import org.cardanofoundation.explorer.consumercommon.entity.StakeAddress;
import org.cardanofoundation.explorer.consumercommon.entity.Withdrawal;
import org.cardanofoundation.job.projection.StakeWithdrawalProjection;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {



  @Query("SELECT tx.hash as txHash, block.time as time, block.epochSlotNo as epochSlotNo,"
      + " tx.fee as fee, block.blockNo as blockNo, block.epochNo as epochNo,"
      + " withdrawal.amount as amount"
      + " FROM Withdrawal withdrawal"
      + " INNER JOIN Tx tx ON withdrawal.tx = tx"
      + " INNER JOIN Block block ON tx.block = block"
      + " WHERE withdrawal.addr = :stakeKey"
      + " AND (block.time >= :fromTime )"
      + " AND (block.time <= :toTime)"
      + " AND ( :txHash IS NULL OR tx.hash = :txHash)")
  Page<StakeWithdrawalProjection> getWithdrawalByAddress(@Param("stakeKey") StakeAddress stakeKey,
                                                         @Param("txHash") String txHash,
                                                         @Param("fromTime") Timestamp fromTime,
                                                         @Param("toTime") Timestamp toTime, Pageable pageable);

  @Query("SELECT withdrawal.tx.id"
      + " FROM Withdrawal withdrawal"
      + " WHERE withdrawal.addr = :stakeKey AND withdrawal.tx.id IN :txIds")
  List<Long> getWithdrawalByAddressAndTxIn(@Param("stakeKey") StakeAddress stakeKey,
                                           @Param("txIds") List<Long> txIds);

}
