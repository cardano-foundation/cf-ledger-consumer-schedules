package org.cardanofoundation.job.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.cardanofoundation.job.repository.DelegationRepository;
import org.cardanofoundation.job.repository.EpochRepository;
import org.cardanofoundation.job.repository.PoolHistoryCheckpointRepository;
import org.cardanofoundation.job.repository.PoolUpdateRepository;
import org.cardanofoundation.job.repository.RewardCheckpointRepository;
import org.cardanofoundation.job.service.FetchRewardDataService;

@Profile("koios")
@Service
@RequiredArgsConstructor
@Log4j2
public class FetchRewardDataFromKoiosServiceImpl implements FetchRewardDataService {

  @Value("${application.api.check-reward.base-url}")
  private String apiCheckRewardUrl;

  @Value("${application.api.check-pool-history.base-url}")
  private String apiCheckPoolHistoryUrl;

  private final RestTemplate restTemplate = new RestTemplate();
  private final RewardCheckpointRepository rewardCheckpointRepository;
  private final DelegationRepository delegationRepository;
  private final EpochRepository epochRepository;
  private final PoolHistoryCheckpointRepository poolHistoryCheckpointRepository;
  private final PoolUpdateRepository poolUpdateRepository;

  @Override
  public Boolean checkRewardAvailable(String stakeKey) {
    return rewardCheckpointRepository.checkRewardByStakeAddressAndEpoch(stakeKey);
  }

  @Override
  public Boolean fetchReward(Set<String> stakeKeySet) {
    var startTime = System.currentTimeMillis();
    Boolean result = restTemplate.postForObject(apiCheckRewardUrl, stakeKeySet, Boolean.class);
    log.info(
        "Time taken to fetch reward for stake key list with size {} is {} ms",
        stakeKeySet.size(),
        System.currentTimeMillis() - startTime);
    return result;
  }

  @Override
  public Boolean fetchReward(String poolView) {
    var startTime = System.currentTimeMillis();

    List<String> stakeKeyList = poolUpdateRepository.findRewardAccountByPoolView(poolView);

    log.info("Total stake keys to fetch reward for pool {} is {}", poolView, stakeKeyList.size());

    int subListSize = 300;
    List<CompletableFuture<Boolean>> fetchRewardResult = new ArrayList<>();

    for (int i = 0; i < stakeKeyList.size(); i += subListSize) {
      int toIndex = Math.min(i + subListSize, stakeKeyList.size());
      List<String> subList = stakeKeyList.subList(i, toIndex);
      fetchRewardResult.add(
          CompletableFuture.supplyAsync(() -> fetchReward(new HashSet<>(subList))));
    }

    Boolean response = fetchRewardResult.stream().allMatch(CompletableFuture::join);

    log.info(
        "Time taken to fetch reward for pool {} is {} ms",
        poolView,
        System.currentTimeMillis() - startTime);
    return response;
  }

  @Override
  public Boolean checkPoolHistoryForPool(Set<String> poolIds) {
    Integer countCheckPoint =
        poolHistoryCheckpointRepository.checkRewardByPoolViewAndEpoch(poolIds);
    Integer sizeCheck = poolIds.size();
    return Objects.equals(countCheckPoint, sizeCheck);
  }

  @Override
  public Boolean fetchPoolHistoryForPool(Set<String> poolIds) {
    return restTemplate.postForObject(apiCheckPoolHistoryUrl, poolIds, Boolean.class);
  }

  @Override
  public Boolean isKoiOs() {
    return true;
  }
}
