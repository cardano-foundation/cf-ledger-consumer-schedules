package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.common.entity.ledgersync.EpochParam;

@Repository
public interface EpochParamRepository extends JpaRepository<EpochParam, Long> {

  List<EpochParam> findByEpochNoIn(@Param("epochNo") List<Integer> epochNo);

  @Query(
      value =
          "SELECT ep FROM EpochParam ep WHERE ep.epochNo = (SELECT MAX(ep2.epochNo) FROM EpochParam ep2)")
  EpochParam findCurrentEpochParam();

  @Query(value = "select ep.drepActivity from EpochParam ep where ep.epochNo = :epochNo")
  Optional<Long> findDRepActivityByEpochNo(@Param("epochNo") Long epochNo);
}
