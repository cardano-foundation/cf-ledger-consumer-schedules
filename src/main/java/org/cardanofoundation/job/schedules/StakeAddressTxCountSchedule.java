package org.cardanofoundation.job.schedules;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.job.service.StakeAddressTxCountService;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
@ConditionalOnProperty(
    value = "jobs.stake-address-tx-count.enabled",
    matchIfMissing = true,
    havingValue = "true")
public class StakeAddressTxCountSchedule {

  private final StakeAddressTxCountService stakeAddressTxCountService;

  @Scheduled(initialDelay = 10000, fixedDelayString = "${jobs.stake-address-tx-count.fixed-delay}")
  @Transactional
  public void updateStakeAddressTxCount() {

    try {
      log.info("Stake Address Tx Count Job: -------Start------");
      long startTime = System.currentTimeMillis();

      stakeAddressTxCountService.updateStakeAddressTxCount();
      log.info(
          "Update the number of tx of stake address successfully, takes: [{} ms]",
          System.currentTimeMillis() - startTime);
      log.info("Stake Address Tx Count Job: -------End------");
    } catch (Exception e) {
      log.error(
          "Error occurred during update the number of tx in stake address: {}", e.getMessage(), e);
    }
  }
}
