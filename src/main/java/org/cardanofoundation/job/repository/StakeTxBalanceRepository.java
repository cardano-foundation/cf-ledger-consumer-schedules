package org.cardanofoundation.job.repository;

import org.cardanofoundation.explorer.consumercommon.entity.StakeTxBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface StakeTxBalanceRepository extends JpaRepository<StakeTxBalance, Long> {

  @Query("SELECT max(stb.txId) FROM StakeTxBalance stb")
  Long getMaxTxId();

  @Transactional
  @Modifying
  @Query(
      value =
          "insert into stake_tx_balance (tx_id, stake_address_id, balance_change, time) "
              + "select atb.tx_id as tx_id, atb.stake_address_id as stake_address_id, "
              + "sum(atb.balance) as balance_change, atb.time as time "
              + "from address_tx_balance atb "
              + "where balance != 0 "
              + "and tx_id >= :txIdSnapshot "
              + "and tx_id <= :txIdTop "
              + "and stake_address_id is not null "
              + "group by (atb.tx_id, atb.time, atb.stake_address_id)",
      nativeQuery = true)
  void insertStakeTxBalance(
      @Param("txIdSnapshot") Long txIdSnapshot, @Param("txIdTop") Long txIdTop);
}
