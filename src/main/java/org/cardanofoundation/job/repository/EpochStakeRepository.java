package org.cardanofoundation.job.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.EpochStake;


@Repository
public interface EpochStakeRepository extends JpaRepository<EpochStake, Long> {

  @Query("SELECT MAX(es.epochNo) FROM EpochStake es")
  Integer findMaxEpochNoStaked();

  List<EpochStake> findEpochStakeByEpochNo(Integer epochNo);
}
