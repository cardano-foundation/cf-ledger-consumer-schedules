package org.cardanofoundation.job.repository.explorer;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.consumercommon.explorer.entity.AggregatePoolInfo;

public interface AggregatePoolInfoRepository extends JpaRepository<AggregatePoolInfo, Long> {

  List<AggregatePoolInfo> findAllByPoolIdIn(Set<Long> poolIds);
}
