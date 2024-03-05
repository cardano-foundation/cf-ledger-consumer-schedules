package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.common.entity.ledgersync.PoolRetire;
import org.cardanofoundation.job.projection.PoolCertificateProjection;
import org.cardanofoundation.job.projection.PoolDeRegistrationProjection;
import org.cardanofoundation.job.projection.PoolUpdateTxProjection;

@Repository
public interface PoolRetireRepository extends JpaRepository<PoolRetire, Long> {

  @Query(
      value =
          "SELECT tx.fee AS fee, pr.retiringEpoch AS retiringEpoch, tx.hash AS txHash, bk.time AS time "
              + "FROM PoolRetire pr "
              + "JOIN Tx tx ON pr.announcedTx.id  = tx.id "
              + "JOIN Block bk ON tx.block.id = bk.id "
              + "WHERE pr.id IN :poolRetiredIds ")
  Page<PoolDeRegistrationProjection> getPoolDeRegistration(
      @Param("poolRetiredIds") Set<Long> poolRetiredIds, Pageable pageable);

  @Query(
      "SELECT  new org.cardanofoundation.job.projection.PoolUpdateTxProjection(poolRetire.announcedTxId, poolRetire.poolHashId, MAX(poolRetire.certIndex)) "
          + "FROM PoolRetire poolRetire "
          + "WHERE (poolRetire.announcedTxId, poolRetire.poolHashId) IN "
          + "(SELECT MAX(pr.announcedTxId), pr.poolHashId FROM PoolRetire pr GROUP BY pr.poolHashId)"
          + "AND poolRetire.retiringEpoch <= :epoch "
          + "GROUP BY poolRetire.announcedTxId, poolRetire.poolHashId")
  List<PoolUpdateTxProjection> getLastPoolRetireTilEpoch(@Param("epoch") int epoch);

  @Query(
      value =
          "SELECT tx.id as txId, tx.hash as txHash, b.epochNo as txEpochNo,"
              + "pr.retiringEpoch as certEpochNo, pr.certIndex as certIndex, pr.id as poolRetireId, "
              + "b.time as blockTime, b.blockNo as blockNo, b.epochSlotNo as epochSlotNo, b.slotNo as slotNo "
              + "FROM PoolRetire pr "
              + "JOIN Tx tx on pr.announcedTx = tx "
              + "JOIN Block b on tx.block = b "
              + "WHERE pr.poolHash.view = :poolViewOrHash "
              + "OR pr.poolHash.hashRaw = :poolViewOrHash ")
  List<PoolCertificateProjection> getPoolRetireByPoolViewOrHash(
      @Param("poolViewOrHash") String poolViewOrHash);
}
