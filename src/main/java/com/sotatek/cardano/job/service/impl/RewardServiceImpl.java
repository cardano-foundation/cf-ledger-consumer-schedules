package com.sotatek.cardano.job.service.impl;

import com.sotatek.cardano.job.service.interfaces.RewardService;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;

@Service
public class RewardServiceImpl implements RewardService {

  @Override
  public void handleReward(LinkedHashMap<String, Object> map) {
    // Do extract reward in here
  }
}
