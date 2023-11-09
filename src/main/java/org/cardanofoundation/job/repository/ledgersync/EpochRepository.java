package org.cardanofoundation.job.repository.ledgersync;

import org.cardanofoundation.job.projection.UniqueAccountTxCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.cardanofoundation.explorer.consumercommon.entity.Epoch;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EpochRepository extends JpaRepository<Epoch, Long> {

  @Query("SELECT MAX(epoch.no) FROM Epoch epoch")
  Integer findMaxEpochNo();

  @Query("""
      SELECT
          (CASE
              WHEN a.stakeAddress IS NULL THEN a.address
              ELSE CAST(a.stakeAddress.id AS STRING)
          END) AS account,
      COUNT(atb) AS txCount
      FROM Block b
      JOIN Tx tx ON tx.block = b
      JOIN AddressTxBalance atb on atb.tx = tx
      JOIN Address a ON atb.address = a
      WHERE b.epochNo = :epochNo
      GROUP BY account
      """)
  List<UniqueAccountTxCountProjection> findUniqueAccountsInEpoch(@Param("epochNo") Integer epochNo);
}
