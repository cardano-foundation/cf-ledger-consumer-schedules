package org.cardanofoundation.job.repository;

import java.sql.Timestamp;
import java.util.Set;

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

  @Query(
      "SELECT tx.hash as txHash, block.time as time, block.epochSlotNo as epochSlotNo,"
          + " poolOfflineData.poolName as poolName, delegation.poolHash.view as poolId,"
          + " block.blockNo as blockNo, block.epochNo as epochNo, tx.fee as fee, tx.outSum as outSum"
          + " FROM Delegation delegation"
          + " INNER JOIN Tx tx ON delegation.tx = tx"
          + " INNER JOIN Block block ON tx.block = block"
          + " LEFT JOIN PoolOfflineData poolOfflineData on poolOfflineData.pool = delegation.poolHash"
          + " WHERE delegation.address = :stakeKey"
          + " AND (block.time >= :fromTime ) "
          + " AND (block.time <= :toTime)"
          + " AND ( :txHash IS NULL OR tx.hash = :txHash)")
  Page<StakeDelegationProjection> findDelegationByAddress(
      @Param("stakeKey") StakeAddress stakeKey,
      @Param("txHash") String txHash,
      @Param("fromTime") Timestamp fromTime,
      @Param("toTime") Timestamp toTime,
      Pageable pageable);

  @Query(
      "SELECT COUNT(d.stakeAddressId)  FROM Delegation d "
          + "LEFT JOIN StakeDeregistration de ON de.addr.id = d.stakeAddressId AND de.txId ="
          + "(SELECT MAX(stakeDereg.txId) FROM StakeDeregistration stakeDereg WHERE "
          + "stakeDereg.addr.id = d.stakeAddressId) "
          + "LEFT JOIN StakeRegistration re ON re.addr.id = d.stakeAddressId AND re.txId = "
          + "(SELECT MAX(stakeReg.txId) FROM StakeRegistration stakeReg WHERE "
          + "stakeReg.addr.id = d.stakeAddressId) "
          + "WHERE d.id IN "
          + "(SELECT MAX(deleIn.id) FROM Delegation deleIn "
          + "GROUP BY (deleIn.stakeAddressId)) "
          + "AND (de.tx.id  < re.tx.id OR de IS NULL) "
          + "AND d.txId >= re.txId "
          + "AND (d.poolHash.id IN :poolIds)")
  Integer countCurrentDelegator(@Param("poolIds") Set<Long> poolIds);
}
