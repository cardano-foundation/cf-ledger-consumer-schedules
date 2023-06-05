package org.cardanofoundation.job.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.Reward;
import org.cardanofoundation.explorer.consumercommon.entity.StakeAddress;
import org.cardanofoundation.job.projection.EpochRewardProjection;
import org.cardanofoundation.job.projection.LifeCycleRewardProjection;
import org.cardanofoundation.job.projection.StakeRewardProjection;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {

  @Query(
      "SELECT rw.spendableEpoch as epoch, e.startTime as time, rw.amount as amount"
          + " FROM Reward rw"
          + " INNER JOIN Epoch e ON rw.spendableEpoch = e.no"
          + " WHERE rw.addr = :stakeAddress"
          + " AND (e.startTime >= :fromDate )"
          + " AND (e.startTime <= :toDate )")
  Page<StakeRewardProjection> findRewardByStake(
      @Param("stakeAddress") StakeAddress stakeAddress,
      @Param("fromDate") Timestamp fromDate,
      @Param("toDate") Timestamp toDate,
      Pageable pageable);

  @Query(
      value =
          "SELECT rw.earnedEpoch AS epochNo, e.startTime AS time, rw.amount AS amount, sa.view AS address "
              + "FROM Reward rw "
              + "JOIN PoolHash ph ON rw.pool.id = ph.id "
              + "JOIN StakeAddress sa ON rw.addr.id = sa.id "
              + "JOIN Epoch e ON rw.spendableEpoch = e.no "
              + "WHERE ph.view  = :poolView AND rw.type = 'leader' "
              + "ORDER BY rw.earnedEpoch DESC")
  Page<LifeCycleRewardProjection> getRewardInfoByPool(
      @Param("poolView") String poolView, Pageable pageable);

  @Query(
      value =
          "SELECT rw.earnedEpoch AS epochNo, rw.amount AS amount "
              + "FROM Reward rw "
              + "JOIN PoolHash ph ON rw.pool.id = ph.id "
              + "WHERE ph.view = :poolView AND rw.type = 'refund' AND rw.earnedEpoch IN :epochNos")
  List<EpochRewardProjection> getRewardRefundByEpoch(
      @Param("poolView") String poolView, @Param("epochNos") Set<Integer> epochNos);
}
