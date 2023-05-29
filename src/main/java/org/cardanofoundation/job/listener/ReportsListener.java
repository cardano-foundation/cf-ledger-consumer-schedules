package org.cardanofoundation.job.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.cardanofoundation.explorer.consumercommon.entity.PoolReportHistory;
import org.cardanofoundation.explorer.consumercommon.entity.ReportHistory;
import org.cardanofoundation.explorer.consumercommon.entity.StakeKeyReportHistory;
import org.cardanofoundation.job.repository.PoolReportHistoryRepository;
import org.cardanofoundation.job.repository.StakeKeyReportHistoryRepository;
import org.cardanofoundation.job.service.PoolReportService;
import org.cardanofoundation.job.service.StakeKeyReportService;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "kafka.configuration-enabled", matchIfMissing = true, havingValue = "true")
public class ReportsListener {

  private final StakeKeyReportHistoryRepository stakeKeyReportHistoryRepository;
  private final PoolReportHistoryRepository poolReportHistoryRepository;
  private final StakeKeyReportService stakeKeyReportService;
  private final PoolReportService poolReportService;

  @KafkaListener(topics = "${kafka.listeners.topics.reports}")
  public void consume(ConsumerRecord<String, ReportHistory> consumerRecord,
                      Acknowledgment acknowledgment) {
    try {
      ReportHistory reportHistory = consumerRecord.value();
      log.info("Receive report history {} with type {}", reportHistory.getId(),
               reportHistory.getType());

      switch (reportHistory.getType()) {
        case STAKE_KEY:
          StakeKeyReportHistory stakeKeyReportHistory = stakeKeyReportHistoryRepository.findByReportHistoryId(
              reportHistory.getId());
          stakeKeyReportService.exportStakeKeyReport(stakeKeyReportHistory);
          break;
        case POOL_ID:
          PoolReportHistory poolReportHistory = poolReportHistoryRepository.findByReportHistoryId(
              reportHistory.getId());
          poolReportService.exportPoolReport(poolReportHistory);
          break;
        default:
          break;
      }

      acknowledgment.acknowledge();
      log.info("Acknowledge report history {}", reportHistory.getId());
    } catch (Exception e) {
      log.error("Consume report history failure: {}", e.getMessage());
    }
  }
}
