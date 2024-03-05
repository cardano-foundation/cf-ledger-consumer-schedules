package org.cardanofoundation.job.schedules;

import java.sql.Timestamp;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.projection.TxInfoProjection;
import org.cardanofoundation.job.repository.ledgersync.StakeTxBalanceRepository;
import org.cardanofoundation.job.repository.ledgersync.TxRepository;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Log4j2
@ConditionalOnProperty(
    value = "jobs.stake-tx-balance.enabled",
    matchIfMissing = true,
    havingValue = "true")
public class StakeTxBalanceSchedule {

  private final TxRepository txRepository;
  private final StakeTxBalanceRepository stakeTxBalanceRepository;
  private static final int MONTH_TO_SUBTRACT = 3;
  private static final int MINUTES_TO_SUBTRACT = 5;
  private static final int DAYS_TO_ADD = 3;

  @Scheduled(fixedDelayString = "${jobs.stake-tx-balance.fixed-delay}")
  void syncStakeTxBalance() {
    log.info("Start job syncStakeTxBalance");
    long startTime = System.currentTimeMillis();
    TxInfoProjection currentTxInfo = txRepository.findCurrentTxInfo();
    Timestamp txBlockTimeAtSnapshotTime =
        Timestamp.valueOf(
            currentTxInfo.getBlockTime().toLocalDateTime().minusMonths(MONTH_TO_SUBTRACT));
    Long maxTxIdStakeTxBalance = stakeTxBalanceRepository.getMaxTxId();
    Long txIdSnapShot;
    Long txIdTop =
        txRepository.findLastTxIdByTxTimeBetween(
            Timestamp.valueOf(
                currentTxInfo.getBlockTime().toLocalDateTime().minusMinutes(MINUTES_TO_SUBTRACT)),
            currentTxInfo.getBlockTime());
    if (Objects.isNull(maxTxIdStakeTxBalance)) {
      txIdSnapShot =
          txRepository.findFirstTxIdByTxTimeBetween(
              txBlockTimeAtSnapshotTime,
              Timestamp.valueOf(txBlockTimeAtSnapshotTime.toLocalDateTime().plusDays(DAYS_TO_ADD)));
    } else {
      txIdSnapShot = maxTxIdStakeTxBalance + 1;
    }
    log.info("Syncing stake tx balance from txId: {}, to txId: {}", txIdSnapShot, txIdTop);
    stakeTxBalanceRepository.insertStakeTxBalance(txIdSnapShot, txIdTop);
    log.info(
        "End Job sync stake tx balance from txId: {}, to txId: {}, Time taken {}ms",
        txIdSnapShot,
        txIdTop,
        System.currentTimeMillis() - startTime);
  }
}
