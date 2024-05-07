package org.cardanofoundation.job.schedules;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.repository.ledgersync.TokenTxCountRepository;
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

  private final TokenTxCountRepository tokenTxCountRepository;

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

  @Scheduled(fixedRate = 1000 * 60 * 15) // 15 minutes
  public void updateNumberOfTokenTx() {
    try {
      log.info("Token Info Job: -------Start------");
      long startTime = System.currentTimeMillis();

      tokenTxCountRepository.refreshMaterializedView();

      long executionTime = System.currentTimeMillis() - startTime;
      log.info("Update number of token transactions successfully, takes: [{} ms]", executionTime);
      log.info("Token Info Job: -------End------");
    } catch (Exception e) {
      log.error("Error occurred during Token Info update: {}", e.getMessage(), e);
    }
  }
}
