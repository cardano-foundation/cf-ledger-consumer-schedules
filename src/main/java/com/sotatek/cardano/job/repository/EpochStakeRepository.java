package com.sotatek.cardano.job.repository;

import com.sotatek.cardano.common.entity.EpochStake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EpochStakeRepository extends JpaRepository<EpochStake, Long> {

  @Query( "SELECT MAX(es.epochNo) FROM EpochStake es")
  Integer findMaxEpochNoStaked();
}
