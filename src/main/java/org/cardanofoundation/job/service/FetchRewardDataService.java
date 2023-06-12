package org.cardanofoundation.job.service;

import java.util.Set;

public interface FetchRewardDataService {

  Boolean checkRewardAvailable(String stakeKey);

  Boolean fetchReward(Set<String> stakeKeySet);

  Boolean fetchReward(String poolView);

  Boolean checkPoolHistoryForPool(Set<String> poolIds);

  Boolean fetchPoolHistoryForPool(Set<String> poolIds);

  Boolean isKoiOs();
}
