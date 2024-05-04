package org.cardanofoundation.job.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.cardanofoundation.job.service.TxCountService;

@Service
@RequiredArgsConstructor
@Log4j2
@ConditionalOnProperty(value = "jobs.address.enabled", matchIfMissing = true, havingValue = "true")
public class AddressSchedule {

  private final TxCountService txCountService;

  @Scheduled(fixedRateString = "${jobs.address.fixed-delay}")
  public void updateTxCountTable() {
    log.info("Start job to update tx count for address");
    long startTime = System.currentTimeMillis();
    txCountService.refreshDataForTxCount();
    long executionTime = System.currentTimeMillis() - startTime;
    log.info("Update tx count for address successfully, takes: [{} ms]", executionTime);
  }
}
