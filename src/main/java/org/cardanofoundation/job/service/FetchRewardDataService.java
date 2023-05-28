package org.cardanofoundation.job.service;

import java.util.Set;

public interface FetchRewardDataService {

  boolean checkRewardAvailable(String stakeKey);

  Boolean fetchReward(Set<String> stakeKeySet);

}
