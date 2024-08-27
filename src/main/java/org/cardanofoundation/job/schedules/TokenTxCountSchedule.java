package org.cardanofoundation.job.schedules;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.service.TokenTxCountService;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class TokenTxCountSchedule {

  private final TokenTxCountService tokenTxCountService;

  @Scheduled(fixedDelayString = "${jobs.token-info.fixed-delay}")
  public void updateTokenTxCount() {
    try {
      log.info("Token Tx Count Job: -------Start------");
      long startTime = System.currentTimeMillis();

      tokenTxCountService.updateTokenTxCount();
      log.info(
          "Update the number of tx of token successfully, takes: [{} ms]",
          System.currentTimeMillis() - startTime);
      log.info("Token Tx Count Job: -------End------");
    } catch (Exception e) {
      log.error("Error occurred during update the number of tx in token: {}", e.getMessage(), e);
    }
  }
}
