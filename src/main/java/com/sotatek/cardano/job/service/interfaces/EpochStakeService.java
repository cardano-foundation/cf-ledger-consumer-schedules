package com.sotatek.cardano.job.service.interfaces;

import com.sotatek.cardano.ledgersync.common.Era;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

public interface EpochStakeService {

  void handleEpoch(Integer epochNo);

  Integer findMaxEpochNoStaked();
}
