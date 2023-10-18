package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.PoolRetire;
import org.cardanofoundation.job.projection.PoolDeRegistrationProjection;
import org.cardanofoundation.job.projection.PoolUpdateTxProjection;

@Repository
public interface PoolRetireRepository extends JpaRepository<PoolRetire, Long> {

  @Query(
      value =
          "SELECT tx.fee AS fee, pr.retiringEpoch AS retiringEpoch, tx.hash AS txHash, bk.time AS time "
              + "FROM PoolRetire pr "
              + "JOIN PoolHash ph ON pr.poolHash.id  = ph.id "
              + "JOIN Tx tx ON pr.announcedTx.id  = tx.id "
              + "JOIN Block bk ON tx.block.id = bk.id "
              + "WHERE ph.view = :poolView ")
  Page<PoolDeRegistrationProjection> getPoolDeRegistration(
      @Param("poolView") String poolView, Pageable pageable);

  @Query(
      "SELECT  new org.cardanofoundation.job.projection.PoolUpdateTxProjection(poolRetire.announcedTxId, poolRetire.poolHashId, MAX(poolRetire.certIndex)) "
          + "FROM PoolRetire poolRetire "
          + "WHERE (poolRetire.announcedTxId, poolRetire.poolHashId) IN "
          + "(SELECT MAX(pr.announcedTxId), pr.poolHashId FROM PoolRetire pr GROUP BY pr.poolHashId)"
          + "AND poolRetire.retiringEpoch <= :epoch "
          + "GROUP BY poolRetire.announcedTxId, poolRetire.poolHashId")
  List<PoolUpdateTxProjection> getLastPoolRetireTilEpoch(@Param("epoch") int epoch);
}
