package org.cardanofoundation.job.schedules;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.repository.ledgersync.CommitteeInfoRepository;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class CommitteeInfoSchedule {

  private final CommitteeInfoRepository committeeInfoRepository;

  @Scheduled(fixedRate = 1000 * 60 * 5) // 5 minutes
  public void refreshMaterializedView() {
    try {
      long startTime = System.currentTimeMillis();
      log.info("Scheduled Committee Info Job: -------Start------");
      committeeInfoRepository.refreshMaterializedView();
      log.info(
          "Update Committee Info successfully, takes: [{} ms]",
          System.currentTimeMillis() - startTime);
      log.info("Scheduled Committee Info Job: -------End------");
    } catch (Exception e) {
      log.error("Error occurred during Token Info update: {}", e.getMessage(), e);
    }
  }
}
