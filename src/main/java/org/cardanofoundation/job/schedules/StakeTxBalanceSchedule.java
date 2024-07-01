package org.cardanofoundation.job.schedules;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.repository.ledgersyncagg.StakeTxBalanceRepository;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Log4j2
@ConditionalOnProperty(
    value = "jobs.stake-tx-balance.enabled",
    matchIfMissing = true,
    havingValue = "true")
public class StakeTxBalanceSchedule {
  private final StakeTxBalanceRepository stakeTxBalanceRepository;

  @Scheduled(fixedDelayString = "${jobs.stake-tx-balance.fixed-delay}")
  void syncStakeTxBalance() {
    log.info("---StakeTxBalance--- Refresh job has been started");
    long startTime = System.currentTimeMillis();
    stakeTxBalanceRepository.refreshMaterializedView();
    log.info(
        "---StakeTxBalance--- Refresh job has been completed in {}ms",
        System.currentTimeMillis() - startTime);
  }
}
