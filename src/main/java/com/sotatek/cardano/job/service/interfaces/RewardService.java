package com.sotatek.cardano.job.service.interfaces;

import java.util.LinkedHashMap;

public interface RewardService {
  void handleReward(LinkedHashMap<String,Object> map);
}
