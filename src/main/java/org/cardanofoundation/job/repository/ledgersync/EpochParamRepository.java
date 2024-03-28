package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;

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
}
