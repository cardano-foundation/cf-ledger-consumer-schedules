package org.cardanofoundation.job.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.PoolRetire;
import org.cardanofoundation.job.projection.PoolDeRegistrationProjection;

@Repository
public interface PoolRetireRepository extends JpaRepository<PoolRetire, Long> {

  @Query(value =
      "SELECT tx.fee AS fee, pr.retiringEpoch AS retiringEpoch, tx.hash AS txHash, bk.time AS time "
          + "FROM PoolRetire pr "
          + "JOIN PoolHash ph ON pr.poolHash.id  = ph.id "
          + "JOIN Tx tx ON pr.announcedTx.id  = tx.id "
          + "JOIN Block bk ON tx.block.id = bk.id "
          + "WHERE ph.view = :poolView ")
  Page<PoolDeRegistrationProjection> getPoolDeRegistration(@Param("poolView") String poolView,
                                                           Pageable pageable);


}
