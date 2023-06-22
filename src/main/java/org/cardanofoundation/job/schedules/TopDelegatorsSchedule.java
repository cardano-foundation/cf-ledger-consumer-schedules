package org.cardanofoundation.job.schedules;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.projection.StakeAddressProjection;
import org.cardanofoundation.job.repository.StakeAddressRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class TopDelegatorsSchedule {
  private static final int MAX_ELEMENT_CACHE = 500;
  private static final String COLON = ":";
  private static final Gson gson = new Gson();

  private final RedisTemplate<String, Object> redisTemplate;
  private final StakeAddressRepository stakeAddressRepository;

  @Value("${application.network}")
  String network;

  @Scheduled(fixedDelayString = "${jobs.top-delegators.fixed-delay}")
  public void buildTopStakeDelegatorCache() {
    String redisKey = RedisKey.REDIS_TOP_STAKE_DELEGATORS.name() + COLON + network;
    long start = System.currentTimeMillis();
    Pageable pageable = PageRequest.of(0, MAX_ELEMENT_CACHE);
    List<StakeAddressProjection> stakeAddressProjections =
        stakeAddressRepository.findStakeAddressOrderByBalance(pageable);

    List<Long> stakeIds =
        stakeAddressProjections.stream().map(StakeAddressProjection::getId).collect(Collectors.toList());

    redisTemplate.opsForValue().set(redisKey, gson.toJson(stakeIds));
    log.info("Build top-stake-delegators cache successfully, takes: [{} ms]", (System.currentTimeMillis() - start));
  }
}