package org.cardanofoundation.job.schedules;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.provider.RedisProvider;
import org.cardanofoundation.job.service.DelegationService;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class DelegationSchedule {
  @Value("${application.network}")
  String network;

  final DelegationService delegatorService;

  final RedisProvider<String, Integer> redisProvider;

  @Scheduled(fixedRateString = "${jobs.number-delegator.fixed-delay}")
  public void updateNumberDelegator() {
    log.info("Update number of delegator!");
    int numberDelegator = delegatorService.countCurrentDelegator();
    String delegatorKey = redisProvider.getRedisKey(RedisKey.TOTAL_DELEGATOR.name());
    redisProvider.setValueByKey(delegatorKey, numberDelegator);
    log.info("Update number of delegator {} successfully", numberDelegator);
  }
}
