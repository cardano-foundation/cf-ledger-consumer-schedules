package org.cardanofoundation.job.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.cardanofoundation.job.service.TxChartService;

@Service
@RequiredArgsConstructor
@Log4j2
@ConditionalOnProperty(value = "jobs.tx-chart.enabled", matchIfMissing = true, havingValue = "true")
public class TxChartSchedule {
  private final TxChartService txChartService;

  @Scheduled(fixedRateString = "${jobs.tx-chart.fixed-delay}")
  public void updateTxChartData() {
    log.info("Start job to update data for tx chart");
    long startTime = System.currentTimeMillis();
    txChartService.refreshDataForTxChart();
    long executionTime = System.currentTimeMillis() - startTime;
    log.info("Update tx chart data successfully, takes: [{} ms]", executionTime);
  }
}
