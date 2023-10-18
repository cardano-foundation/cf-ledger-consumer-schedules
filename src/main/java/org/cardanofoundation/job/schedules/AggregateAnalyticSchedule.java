package org.cardanofoundation.job.schedules;

import java.sql.Timestamp;
import java.time.*;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.repository.ledgersync.AddressTxBalanceRepository;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.aggregate.AggregateAddressTokenRepository;
import org.cardanofoundation.job.repository.ledgersync.aggregate.AggregateAddressTxBalanceRepository;

@Component
@Slf4j
@RequiredArgsConstructor
public class AggregateAnalyticSchedule {

  private static final LocalDate START_DATE = LocalDate.of(2016, 12, 31);

  private final AggregateAddressTokenRepository aggregateAddressTokenRepository;
  private final AggregateAddressTxBalanceRepository aggregateAddressTxBalanceRepository;
  private final BlockRepository blockRepository;
  private final AddressTxBalanceRepository addressTxBalanceRepository;
  private static final int FINALIZE_MINUTE = 15;

  private void runJob(
      Supplier<Optional<LocalDate>> currentMaxDayAggSupplier,
      Supplier<Optional<Timestamp>> currentMaxDayConsumerSupplier,
      BiConsumer<Timestamp, Timestamp> insertFunc,
      String jobName) {
    LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
    long start = System.currentTimeMillis();
    LocalDate currentMaxDay = currentMaxDayAggSupplier.get().orElse(START_DATE);
    if (yesterday.isBefore(currentMaxDay) || yesterday.equals(currentMaxDay)) {
      log.info("All ready had data. Stop running job");
      return;
    }
    Optional<Timestamp> currentMaxTimeConsumer = currentMaxDayConsumerSupplier.get();
    if (currentMaxTimeConsumer.isEmpty()) {
      log.info("No data to aggregate. Stop running job");
      return;
    }

    LocalDate finalizeTimeConsumer =
        currentMaxTimeConsumer
            .get()
            .toLocalDateTime()
            .minusDays(1)
            .minusMinutes(FINALIZE_MINUTE)
            .toLocalDate();
    if (finalizeTimeConsumer.isBefore(currentMaxDay)) {
      log.info(
          "Do not meet finalize time yet: finalize time {}, current max day {}",
          finalizeTimeConsumer,
          currentMaxDay);
      return;
    }

    Timestamp startRange = Timestamp.valueOf(currentMaxDay.plusDays(1).atTime(LocalTime.MIN));

    Timestamp endRange = Timestamp.valueOf(finalizeTimeConsumer.atTime(LocalTime.MAX));
    if (startRange.compareTo(endRange) >= 0) {
      log.info("Time range is invalid. Stop running job");
      return;
    }

    log.info("Start running job: [{}] for from: [{}] to: [{}]", jobName, startRange, endRange);
    insertFunc.accept(startRange, endRange);
    long timeExec = System.currentTimeMillis() - start;
    log.info("Run job [{}] successfully, time exec: [{} ms]", jobName, timeExec);
  }

  @Scheduled(
      cron = "0 20 0 * * *",
      zone = "UTC") // midnight utc 0:20 AM make sure that it will not rollback to block has time <
  // midnight
  public void sumAggBalanceAddressToken() {
    runJob(
        aggregateAddressTokenRepository::getMaxDay,
        blockRepository::getMaxTime,
        aggregateAddressTokenRepository::insertDataForDay,
        "sumAggBalanceAddressToken");
  }

  @Scheduled(
      cron = "0 20 0 * * *",
      zone = "UTC") // midnight utc 0:20 AM make sure that it will not rollback to block has time <
  // midnight
  public void sumAggBalanceAddressTx() {
    runJob(
        aggregateAddressTxBalanceRepository::getMaxDay,
        addressTxBalanceRepository::getMaxTime,
        aggregateAddressTxBalanceRepository::insertDataForDay,
        "sumAggBalanceAddressTx");
  }

  @PostConstruct
  public void checkOnStart() {
    runJob(
        aggregateAddressTokenRepository::getMaxDay,
        blockRepository::getMaxTime,
        aggregateAddressTokenRepository::insertDataForDay,
        "sumAggBalanceAddressToken");

    runJob(
        aggregateAddressTxBalanceRepository::getMaxDay,
        addressTxBalanceRepository::getMaxTime,
        aggregateAddressTxBalanceRepository::insertDataForDay,
        "sumAggBalanceAddressTx");
  }
}
