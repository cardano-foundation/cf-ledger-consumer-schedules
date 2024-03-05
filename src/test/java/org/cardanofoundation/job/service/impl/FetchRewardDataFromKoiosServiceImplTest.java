package org.cardanofoundation.job.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.job.repository.ledgersync.PoolHistoryCheckpointRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolUpdateRepository;
import org.cardanofoundation.job.repository.ledgersync.RewardCheckpointRepository;
import org.cardanofoundation.job.service.FetchRewardDataService;

@ExtendWith(MockitoExtension.class)
class FetchRewardDataFromKoiosServiceImplTest {

  @Mock RewardCheckpointRepository rewardCheckpointRepository;
  @Mock PoolHistoryCheckpointRepository poolHistoryCheckpointRepository;
  @Mock PoolUpdateRepository poolUpdateRepository;
  @Mock RestTemplate restTemplate;
  FetchRewardDataService fetchRewardDataService;
  String apiCheckRewardUrl = "http://localhost:8080/api/check-reward";
  String apiCheckPoolHistoryUrl = "http://localhost:8080/api/check-pool-history";

  @BeforeEach
  void setUp() {
    fetchRewardDataService =
        new FetchRewardDataFromKoiosServiceImpl(
            rewardCheckpointRepository, poolHistoryCheckpointRepository, poolUpdateRepository);
    ReflectionTestUtils.setField(fetchRewardDataService, "apiCheckRewardUrl", apiCheckRewardUrl);
    ReflectionTestUtils.setField(
        fetchRewardDataService, "apiCheckPoolHistoryUrl", apiCheckPoolHistoryUrl);
    ReflectionTestUtils.setField(fetchRewardDataService, "restTemplate", restTemplate);
  }

  @Test
  void checkRewardAvailableTest() {
    final String stakeKey = "stake1u8n707tj5wr83na6dw3lcwzlluu5p0yq88mh4gg6z02htxql0rp92";
    fetchRewardDataService.checkRewardAvailable(stakeKey);
    verify(rewardCheckpointRepository).checkRewardByStakeAddressAndEpoch(stakeKey);
  }

  @Test
  void fetchRewardStakeKeySetTest() {
    final Set<String> stakeKeySet =
        Collections.singleton("stake1u8n707tj5wr83na6dw3lcwzlluu5p0yq88mh4gg6z02htxql0rp92");
    when(restTemplate.postForObject(apiCheckRewardUrl, stakeKeySet, Boolean.class))
        .thenReturn(Boolean.TRUE);
    assertTrue(fetchRewardDataService.fetchReward(stakeKeySet));
    verify(restTemplate).postForObject(apiCheckRewardUrl, stakeKeySet, Boolean.class);
  }

  @Test
  void fetchRewardPoolViewTest() {
    final String poolView = "pool1u8n707tj5wr83na6dw3lcwzlluu5p0yq88mh4gg6z02htxql0rp92";
    final Set<String> stakeKeySet =
        Collections.singleton("stake1u8n707tj5wr83na6dw3lcwzlluu5p0yq88mh4gg6z02htxql0rp92");
    when(poolUpdateRepository.findRewardAccountByPoolView(poolView))
        .thenReturn(stakeKeySet.stream().toList());
    when(restTemplate.postForObject(apiCheckRewardUrl, stakeKeySet, Boolean.class))
        .thenReturn(Boolean.TRUE);
    assertTrue(fetchRewardDataService.fetchReward(poolView));
  }

  @Test
  void checkPoolHistoryForPoolTest() {
    final Set<String> poolIds =
        Collections.singleton("pool1u8n707tj5wr83na6dw3lcwzlluu5p0yq88mh4gg6z02htxql0rp92");
    when(poolHistoryCheckpointRepository.checkRewardByPoolViewAndEpoch(poolIds)).thenReturn(1);
    assertTrue(fetchRewardDataService.checkPoolHistoryForPool(poolIds));
  }

  @Test
  void fetchPoolHistoryForPoolTest() {
    final Set<String> poolIds =
        Collections.singleton("pool1u8n707tj5wr83na6dw3lcwzlluu5p0yq88mh4gg6z02htxql0rp92");
    fetchRewardDataService.fetchPoolHistoryForPool(poolIds);
    verify(restTemplate).postForObject(apiCheckPoolHistoryUrl, poolIds, Boolean.class);
  }

  @Test
  void isKoiOsTest() {
    assertTrue(fetchRewardDataService.isKoiOs());
  }
}
