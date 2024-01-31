package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.cardanofoundation.explorer.consumercommon.entity.PoolOfflineData;

public interface PoolOfflineDataRepository extends JpaRepository<PoolOfflineData, Long> {

  List<PoolOfflineData> findPoolOfflineDataByPoolIdIn(List<Long> poolIds);
}
