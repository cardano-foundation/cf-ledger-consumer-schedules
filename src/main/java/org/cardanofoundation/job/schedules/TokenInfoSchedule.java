package org.cardanofoundation.job.schedules;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.service.TokenInfoService;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
@ConditionalOnProperty(
    value = "jobs.token-info.enabled",
    matchIfMissing = true,
    havingValue = "true")
public class TokenInfoSchedule {

  final TokenInfoService tokenInfoService;

  @Scheduled(fixedDelayString = "${jobs.token-info.fixed-delay}")
  public void updateTokenInfo() {
    log.info("Token Info Job: -------Start------");
    long start = System.currentTimeMillis();
    tokenInfoService.updateTokenInfoList();
    log.info(
        "Update token info successfully, takes: [{} ms]", (System.currentTimeMillis() - start));
    log.info("Token Info Job: -------End------");
  }

}
