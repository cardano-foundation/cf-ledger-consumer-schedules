package org.cardanofoundation.job.schedules;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.BiConsumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.repository.aggregate.AggregateAddressTokenRepository;
import org.cardanofoundation.job.repository.aggregate.AggregateAddressTxBalanceRepository;

@Component
@Slf4j
@RequiredArgsConstructor
public class AggregateAnalyticSchedule {

  private final AggregateAddressTokenRepository aggregateAddressTokenRepository;
  private final AggregateAddressTxBalanceRepository aggregateAddressTxBalanceRepository;

  private void runJob(BiConsumer<Timestamp, Timestamp> insertFunc, String jobName) {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    log.info("Start running job: [{}] for day: [{}]", jobName, yesterday);
    long start = System.currentTimeMillis();
    Timestamp startOfDay = Timestamp.valueOf(yesterday.atTime(LocalTime.MIN));
    Timestamp endOfDay = Timestamp.valueOf(yesterday.atTime(LocalTime.MAX));
    insertFunc.accept(startOfDay, endOfDay);
    long timeExec = System.currentTimeMillis() - start;
    log.info("Run job [{}] successfully, time exec: [{} ms]", jobName, timeExec);
  }

  @Scheduled(cron = "0 0 0 * * *", zone = "UTC") // midnight utc
  public void sumBalanceTokenOfDay() {
    runJob(aggregateAddressTokenRepository::insertDataForDay, "sumBalanceTokenOfDay");
  }

  @Scheduled(cron = "0 0 0 * * *", zone = "UTC") // midnight utc
  public void sumBalanceAddressOfDay() {
    runJob(aggregateAddressTxBalanceRepository::insertDataForDay, "sumBalanceAddressOfDay");
  }
}
