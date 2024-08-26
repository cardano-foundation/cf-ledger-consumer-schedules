package org.cardanofoundation.job.schedules;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.job.service.TokenInfoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    try {
      log.info("Token Info Job: -------Start------");
      long startTime = System.currentTimeMillis();

      tokenInfoService.updateTokenInfoList();

      long executionTime = System.currentTimeMillis() - startTime;
      log.info("Update token info successfully, takes: [{} ms]", executionTime);
      log.info("Token Info Job: -------End------");
    } catch (Exception e) {
      log.error("Error occurred during Token Info update: {}", e.getMessage(), e);
    }
  }
}
