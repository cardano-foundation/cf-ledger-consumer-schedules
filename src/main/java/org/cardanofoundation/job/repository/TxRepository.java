package org.cardanofoundation.job.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.consumercommon.entity.Tx;

public interface TxRepository extends JpaRepository<Tx, Long>, JpaSpecificationExecutor<Tx> {

  @Query("SELECT tx FROM Tx tx WHERE tx.id IN :ids ORDER BY tx.blockId DESC, tx.blockIndex DESC")
  List<Tx> findByIdIn(@Param("ids") List<Long> ids);

  @Query(
      "SELECT min(tx.id) FROM Tx tx "
          + " INNER JOIN Block b ON b.id = tx.blockId"
          + " WHERE b.time >= :time AND b.txCount > 0")
  Optional<Long> findMinTxByAfterTime(@Param("time") Timestamp time);
}
