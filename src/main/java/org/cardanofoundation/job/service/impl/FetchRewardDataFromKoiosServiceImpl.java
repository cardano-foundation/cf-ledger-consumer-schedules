package org.cardanofoundation.job.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import jakarta.validation.constraints.Max;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.cardanofoundation.explorer.consumercommon.entity.Epoch;
import org.cardanofoundation.explorer.consumercommon.entity.StakeAddress;
import org.cardanofoundation.job.repository.DelegationRepository;
import org.cardanofoundation.job.repository.EpochRepository;
import org.cardanofoundation.job.repository.RewardCheckpointRepository;
import org.cardanofoundation.job.service.FetchRewardDataService;


@Profile("koios")
@Service
@RequiredArgsConstructor
@Log4j2
public class FetchRewardDataFromKoiosServiceImpl implements FetchRewardDataService {

  @Value("${application.api.check-reward.base-url}")
  private String apiCheckRewardUrl;

  private final RestTemplate restTemplate = new RestTemplate();
  private final RewardCheckpointRepository rewardCheckpointRepository;
  private final DelegationRepository delegationRepository;
  private final EpochRepository epochRepository;


  @Override
  public boolean checkRewardAvailable(String stakeKey) {
    return rewardCheckpointRepository.checkRewardByStakeAddressAndEpoch(stakeKey);
  }

  @Override
  public Boolean fetchReward(Set<String> stakeKeySet) {
    return restTemplate.postForObject(apiCheckRewardUrl, stakeKeySet, Boolean.class);
  }

  @Override
  public Boolean fetchReward(String poolView) {
    var startTime = System.currentTimeMillis();
    var maxEpochReward = Math.max(0,epochRepository.findMaxEpochNo() - 1);

    List<String> stakeKeyList = delegationRepository
        .findStakeAddressByPoolViewAndRewardCheckPoint(poolView, maxEpochReward)
        .stream().map(StakeAddress::getView).collect(Collectors.toList());

    log.info("Total stake keys to fetch reward for pool {} is {}", poolView, stakeKeyList.size());

    int subListSize = 300;
    List<CompletableFuture<Boolean>> fetchRewardResult = new ArrayList<>();

    for (int i = 0; i < stakeKeyList.size(); i += subListSize) {
      int toIndex = Math.min(i + subListSize, stakeKeyList.size());
      List<String> subList = stakeKeyList.subList(i, toIndex);
      fetchRewardResult.add(CompletableFuture.supplyAsync(
          () -> fetchReward(new HashSet<>(subList))));
    }

    Boolean response = fetchRewardResult.stream().allMatch(CompletableFuture::join);

    log.info("Time taken to fetch reward for pool {} is {} ms", poolView,
             System.currentTimeMillis() - startTime);
    return response;
  }
}
