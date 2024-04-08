package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.common.entity.ledgersync.AggregatePoolInfo;

public interface AggregatePoolInfoRepository extends JpaRepository<AggregatePoolInfo, Long> {

  List<AggregatePoolInfo> findAllByPoolIdIn(Set<Long> poolIds);
}
