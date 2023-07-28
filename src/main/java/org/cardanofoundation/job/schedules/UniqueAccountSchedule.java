package org.cardanofoundation.job.schedules;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.explorer.consumercommon.entity.Epoch;
import org.cardanofoundation.job.projection.UniqueAccountTxCountProjection;
import org.cardanofoundation.job.repository.EpochRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "jobs.unique-account.enabled",
    matchIfMissing = true,
    havingValue = "true")
public class UniqueAccountSchedule {

  private final EpochRepository epochRepository;

  private final RedisTemplate<String, Object> redisTemplate;

  private static final String UNIQUE_ACCOUNTS_KEY = "UNIQUE_ACCOUNTS";

  private static final String UNDERSCORE = "_";

  @Value("${application.network}")
  String network;

  @Scheduled(fixedDelayString = "${jobs.unique-account.fixed-delay}")
  public void buildUniqueAccountEpoch() {

    List<Epoch> epochs = epochRepository.findAll();
    Integer currentEpoch = Collections.max(epochs.stream().map(Epoch::getNo).toList());

    for (Epoch epoch : epochs) {
      final String redisKey = String.join(
          UNDERSCORE,
          getRedisKey(UNIQUE_ACCOUNTS_KEY),
          epoch.getNo().toString());
      var uniqueAccount = redisTemplate.hasKey(redisKey);
      if(epoch.getNo().equals(currentEpoch) || Boolean.TRUE.equals(uniqueAccount)) {
        continue;
      }
      log.info("Building unique account for epoch: {}", epoch.getNo());
      Map<String, Integer> uniqueAccounts = epochRepository
          .findUniqueAccountsInEpoch(epoch.getNo())
          .stream()
          .collect(Collectors.toMap(
              UniqueAccountTxCountProjection::getAccount,
              UniqueAccountTxCountProjection::getTxCount
          ));
      if (!CollectionUtils.isEmpty(uniqueAccounts)) {
        redisTemplate.opsForHash().putAll(redisKey, uniqueAccounts);
      }
      log.info("Building unique account for epoch: {} done", epoch.getNo());
    }
    log.info("Building unique account for all epoch done");
  }

  private String getRedisKey(String key) {
    return String.join(UNDERSCORE, network.toUpperCase(), key);
  }
}