package org.cardanofoundation.job.service.impl;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Service;

import org.cardanofoundation.job.service.interfaces.RewardService;

@Service
public class RewardServiceImpl implements RewardService {

  @Override
  public void handleReward(LinkedHashMap<String, Object> map) {
    // Do extract reward in here
  }
}
