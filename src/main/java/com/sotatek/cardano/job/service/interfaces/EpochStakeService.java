package com.sotatek.cardano.job.service.interfaces;

import java.util.LinkedHashMap;

public interface EpochStakeService {
  void handleEpochStake(LinkedHashMap<String,Object> map);
}
