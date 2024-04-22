package org.cardanofoundation.job.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.cardanofoundation.explorer.common.entity.explorer.PoolReportHistory;
import org.cardanofoundation.explorer.common.entity.explorer.ReportHistory;
import org.cardanofoundation.explorer.common.entity.explorer.StakeKeyReportHistory;
import org.cardanofoundation.explorer.common.model.ReportMessage;
import org.cardanofoundation.job.repository.explorer.PoolReportHistoryRepository;
import org.cardanofoundation.job.repository.explorer.StakeKeyReportHistoryRepository;
import org.cardanofoundation.job.service.PoolReportService;
import org.cardanofoundation.job.service.StakeKeyReportService;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "kafka.configuration-enabled",
    matchIfMissing = true,
    havingValue = "true")
public class ReportsListener {

  private final StakeKeyReportHistoryRepository stakeKeyReportHistoryRepository;
  private final PoolReportHistoryRepository poolReportHistoryRepository;
  private final StakeKeyReportService stakeKeyReportService;
  private final PoolReportService poolReportService;

  @KafkaListener(topics = "${kafka.listeners.topics.reports}")
  public void consume(
      ConsumerRecord<String, ReportMessage> consumerRecord, Acknowledgment acknowledgment) {
    try {
      ReportMessage reportMessage = consumerRecord.value();
      ReportHistory reportHistory = reportMessage.getReportHistory();
      log.info(
          "Receive report history {} with type {}", reportHistory.getId(), reportHistory.getType());

      switch (reportHistory.getType()) {
        case STAKE_KEY:
          StakeKeyReportHistory stakeKeyReportHistory =
              stakeKeyReportHistoryRepository.findByReportHistoryId(reportHistory.getId());
          stakeKeyReportService.exportStakeKeyReport(
              stakeKeyReportHistory, reportMessage.getZoneOffset(), reportMessage.getTimePattern());
          break;
        case POOL_ID:
          PoolReportHistory poolReportHistory =
              poolReportHistoryRepository.findByReportHistoryId(reportHistory.getId());
          poolReportService.exportPoolReport(
              poolReportHistory, reportMessage.getZoneOffset(), reportMessage.getTimePattern());
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
