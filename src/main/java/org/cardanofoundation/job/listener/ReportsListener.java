package org.cardanofoundation.job.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.cardanofoundation.explorer.consumercommon.entity.ReportHistory;
import org.cardanofoundation.explorer.consumercommon.entity.StakeKeyReportHistory;
import org.cardanofoundation.job.repository.StakeKeyReportHistoryRepository;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReportsListener {

  private final StakeKeyReportHistoryRepository stakeKeyReportHistoryRepository;

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


          break;
        case POOL_ID:
          break;
        default:
          break;
      }
      acknowledgment.acknowledge();
    } catch (Exception e) {
      e.printStackTrace();
      log.error(e.getMessage());
    }
  }
}
