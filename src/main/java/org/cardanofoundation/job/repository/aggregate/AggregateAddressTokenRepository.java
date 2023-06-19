package org.cardanofoundation.job.repository.aggregate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.consumercommon.entity.aggregation.AggregateAddressToken;

public interface AggregateAddressTokenRepository
    extends JpaRepository<AggregateAddressToken, Long> {

  @Transactional
  @Modifying
  @Query(
      value =
          "insert into agg_address_token (ident, balance, day) "
              + "SELECT addt.ident as ident, "
              + "       sum(addt.balance) as sum_balance, "
              + "       date_trunc('day', b.time) as time_agg "
              + "FROM address_token addt "
              + "inner join multi_asset ma on addt.ident = ma.id "
              + "inner join tx t on addt.tx_id = t.id "
              + "inner join block b on t.block_id = b.id "
              + "where b.time >= :startOfDay "
              + "and b.time <= :endOfDay "
              + "and b.tx_count > 0 and addt.balance > 0 "
              + "GROUP BY addt.ident, time_agg "
              + "order by time_agg",
      nativeQuery = true)
  void insertDataForDay(
      @Param("startOfDay") Timestamp startOfDay, @Param("endOfDay") Timestamp endOfDay);

  @Query("select max(a.day) from AggregateAddressToken a")
  Optional<LocalDate> getMaxDay();
}
