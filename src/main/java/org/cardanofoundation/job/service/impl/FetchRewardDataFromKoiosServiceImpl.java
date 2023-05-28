package org.cardanofoundation.job.service.impl;

import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.cardanofoundation.job.repository.RewardCheckpointRepository;
import org.cardanofoundation.job.service.FetchRewardDataService;


@Profile("koios")
@Service
@RequiredArgsConstructor
public class FetchRewardDataFromKoiosServiceImpl implements FetchRewardDataService {

  @Value("${application.api.check-reward.base-url}")
  private String apiCheckRewardUrl;

  private final RestTemplate restTemplate = new RestTemplate();
  private final RewardCheckpointRepository rewardCheckpointRepository;


  @Override
  public boolean checkRewardAvailable(String stakeKey) {
    return rewardCheckpointRepository.checkRewardByStakeAddressAndEpoch(stakeKey);
  }

  @Override
  public Boolean fetchReward(Set<String> stakeKeySet) {
    return restTemplate.postForObject(apiCheckRewardUrl, stakeKeySet, Boolean.class);
  }
}
