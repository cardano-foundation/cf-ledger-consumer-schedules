package org.cardanofoundation.job.repository.aggregate;

import java.sql.Timestamp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.consumercommon.entity.aggregation.AggregateAddressTxBalance;

public interface AggregateAddressTxBalanceRepository
    extends JpaRepository<AggregateAddressTxBalance, Long> {

  @Transactional
  @Modifying
  @Query(
      value =
          "insert into agg_address_tx_balance (stake_address_id, address_id, balance, day) "
              + "SELECT addr.stake_address_id       as stake_address_id, "
              + "       addr.id                     as address_id, "
              + "       sum(addr.balance)           as sum_balance, "
              + "       date_trunc('day', atb.time) as time_agg "
              + "FROM address_tx_balance atb "
              + "inner join address addr on atb.address_id = addr.id "
              + "where atb.time >= :startOfDay "
              + "and atb.time <= :endOfDay "
              + "GROUP BY addr.id, time_agg "
              + "order by time_agg",
      nativeQuery = true)
  void insertDataForDay(
      @Param("startOfDay") Timestamp startOfDay, @Param("endOfDay") Timestamp endOfDay);
}
