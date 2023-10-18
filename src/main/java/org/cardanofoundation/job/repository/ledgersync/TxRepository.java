package org.cardanofoundation.job.repository.ledgersync;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.cardanofoundation.explorer.consumercommon.entity.Tx;
import org.cardanofoundation.job.projection.TxInfoProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TxRepository extends JpaRepository<Tx, Long>, JpaSpecificationExecutor<Tx> {

  @Query("SELECT tx FROM Tx tx WHERE tx.id IN :ids ORDER BY tx.blockId DESC, tx.blockIndex DESC")
  List<Tx> findByIdIn(@Param("ids") List<Long> ids);

  @Query(
      "SELECT min(tx.id) FROM Tx tx "
          + " INNER JOIN Block b ON b.id = tx.blockId"
          + " WHERE b.time >= :time AND b.txCount > 0")
  Optional<Long> findMinTxByAfterTime(@Param("time") Timestamp time);

  @Query(
      "SELECT tx.id as txId, b.time as blockTime FROM Tx tx "
          + "JOIN Block b on b.id = tx.blockId "
          + "WHERE tx.id = (SELECT max(tx.id) FROM Tx tx)")
  TxInfoProjection findCurrentTxInfo();

  @Query(
      "SELECT min(tx.id) from Tx tx "
          + "JOIN Block b on b.id = tx.blockId "
          + "WHERE b.time >= :fromTime AND b.time <= :toTime")
  Long findFirstTxIdByTxTimeBetween(
      @Param("fromTime") Timestamp fromTime, @Param("toTime") Timestamp toTime);

  @Query(
      "SELECT max(tx.id) from Tx tx "
          + "JOIN Block b on b.id = tx.blockId "
          + "WHERE b.time >= :fromTime AND b.time <= :toTime")
  Long findLastTxIdByTxTimeBetween(
      @Param("fromTime") Timestamp fromTime, @Param("toTime") Timestamp toTime);
}
