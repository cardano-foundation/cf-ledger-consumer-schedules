package org.cardanofoundation.job.service.impl;

import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.cardanofoundation.job.service.FetchRewardDataService;

@Profile("!koios")
@Service
@RequiredArgsConstructor
public class FetchRewardDataServiceImpl implements FetchRewardDataService {

  @Override
  public Boolean checkRewardAvailable(String stakeKey) {
    return true;
  }

  @Override
  public Boolean fetchReward(Set<String> stakeKeySet) {
    return true;
  }

  @Override
  public Boolean fetchReward(String poolView) {
    return true;
  }

  @Override
  public Boolean checkPoolHistoryForPool(Set<String> poolIds) {
    return true;
  }

  @Override
  public Boolean fetchPoolHistoryForPool(Set<String> poolIds) {
    return true;
  }

  @Override
  public Boolean isKoiOs() {
    return false;
  }
}
