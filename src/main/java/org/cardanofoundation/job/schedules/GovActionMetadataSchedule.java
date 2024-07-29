package org.cardanofoundation.job.schedules;

import java.util.Queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.dto.govActionMetaData.OffChainGovActionData;
import org.cardanofoundation.job.service.OffChainVoteGovActionDataFetchingService;
import org.cardanofoundation.job.service.OffChainVoteGovActionDataStoringService;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "jobs.gov-action-metadata.enabled",
    matchIfMissing = true,
    havingValue = "true")
public class GovActionMetadataSchedule {

  private final OffChainVoteGovActionDataFetchingService consumeOffChainVoteGovActionDataService;
  private final OffChainVoteGovActionDataStoringService offChainVoteGovActionDataStoringService;

  /** Fetch gov action metadata. */
  @Scheduled(fixedRateString = "${jobs.gov-action-metadata.fixed-delay}")
  public void fetchGovActionMetadata() {
    long startTime = System.currentTimeMillis();
    log.info("Start fetching gov action metadata");
    Queue<OffChainGovActionData> offChainGovActionDataList =
        consumeOffChainVoteGovActionDataService.getDataFromAnchorUrl();
    offChainVoteGovActionDataStoringService.insertData(offChainGovActionDataList);
    log.info(
        "End fetching gov action metadata, taken time: {} ms",
        System.currentTimeMillis() - startTime);
  }

  /** Retry fetch gov action metadata. */
  @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
  public void retryFetchGovActionMetadata() {
    long startTime = System.currentTimeMillis();
    log.info("Start retry fetching gov action metadata");
    consumeOffChainVoteGovActionDataService.retryFetchGovActionMetadata();
    log.info(
        "End retry fetching gov action metadata, taken time: {} ms",
        System.currentTimeMillis() - startTime);
  }
}
