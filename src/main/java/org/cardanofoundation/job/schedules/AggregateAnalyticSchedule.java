package org.cardanofoundation.job.schedules;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.repository.AddressTxBalanceRepository;
import org.cardanofoundation.job.repository.BlockRepository;
import org.cardanofoundation.job.repository.aggregate.AggregateAddressTokenRepository;
import org.cardanofoundation.job.repository.aggregate.AggregateAddressTxBalanceRepository;

@Component
@Slf4j
@RequiredArgsConstructor
public class AggregateAnalyticSchedule {

  private static final LocalDate START_DATE = LocalDate.of(2016, 12, 31);

  private final AggregateAddressTokenRepository aggregateAddressTokenRepository;
  private final AggregateAddressTxBalanceRepository aggregateAddressTxBalanceRepository;
  private final BlockRepository blockRepository;
  private final AddressTxBalanceRepository addressTxBalanceRepository;

  private void runJob(
      Supplier<Optional<LocalDate>> currentMaxDayAggSupplier,
      Supplier<Optional<Timestamp>> currentMaxDayConsumerSupplier,
      BiConsumer<Timestamp, Timestamp> insertFunc,
      String jobName) {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    long start = System.currentTimeMillis();

    LocalDate currentMaxDay = currentMaxDayAggSupplier.get().orElse(START_DATE);
    if (currentMaxDay.isEqual(yesterday)) {
      log.info("All ready had data. Stop running job");
      return;
    }
    Optional<Timestamp> currentMaxTimeConsumer = currentMaxDayConsumerSupplier.get();
    if (currentMaxTimeConsumer.isEmpty()) {
      log.info("No data to aggregate. Stop running job");
      return;
    }

    Timestamp startRange = Timestamp.valueOf(currentMaxDay.plusDays(1).atTime(LocalTime.MIN));

    LocalDate dayBeforeMaxConsumeTime =
        currentMaxTimeConsumer.get().toLocalDateTime().toLocalDate().minusDays(1);
    Timestamp endRange = Timestamp.valueOf(dayBeforeMaxConsumeTime.atTime(LocalTime.MAX));
    if (startRange.equals(endRange)) {
      log.info("Time range is invalid. Stop running job");
      return;
    }

    log.info("Start running job: [{}] for from: [{}] to: [{}]", jobName, startRange, endRange);
    insertFunc.accept(startRange, endRange);
    long timeExec = System.currentTimeMillis() - start;
    log.info("Run job [{}] successfully, time exec: [{} ms]", jobName, timeExec);
  }

  @Scheduled(cron = "0 0 0 * * *", zone = "UTC") // midnight utc
  public void sumAggBalanceAddressToken() {
    runJob(
        aggregateAddressTokenRepository::getMaxDay,
        blockRepository::getMaxTime,
        aggregateAddressTokenRepository::insertDataForDay,
        "sumAggBalanceAddressToken");
  }

  @Scheduled(cron = "0 0 0 * * *", zone = "UTC") // midnight utc
  public void sumAggBalanceAddressTx() {
    runJob(
        aggregateAddressTxBalanceRepository::getMaxDay,
        addressTxBalanceRepository::getMaxTime,
        aggregateAddressTxBalanceRepository::insertDataForDay,
        "sumAggBalanceAddressTx");
  }
}
