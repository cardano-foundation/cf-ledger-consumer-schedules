package org.cardanofoundation.job.schedules;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.lang.Collections;

import org.cardanofoundation.explorer.common.entity.enumeration.DataCheckpointType;
import org.cardanofoundation.explorer.common.entity.explorer.DataCheckpoint;
import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteFetchError;
import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteGovActionData;
import org.cardanofoundation.job.dto.govActionMetaData.Anchor;
import org.cardanofoundation.job.repository.explorer.DataCheckpointRepository;
import org.cardanofoundation.job.repository.ledgersync.GovActionProposalRepository;
import org.cardanofoundation.job.service.impl.OffChainVoteGovActionFetchingDataServiceImpl;
import org.cardanofoundation.job.service.impl.OffChainVoteGovActionRetryServiceImpl;
import org.cardanofoundation.job.service.impl.OffChainVoteGovActionStoringServiceImpl;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "jobs.gov-action-metadata.enabled",
    matchIfMissing = true,
    havingValue = "true")
public class GovActionMetadataSchedule {

  private final OffChainVoteGovActionFetchingDataServiceImpl fetchingDataService;
  private final OffChainVoteGovActionRetryServiceImpl retryDataService;

  private final OffChainVoteGovActionStoringServiceImpl storingDataService;
  private final DataCheckpointRepository dataCheckpointRepository;
  private final GovActionProposalRepository govActionProposalRepository;

  @Value("${jobs.gov-action-metadata.retry-count}")
  private int retryCount;

  /** Fetch gov action metadata. */
  @Scheduled(fixedRateString = "${jobs.gov-action-metadata.fixed-delay}")
  @Transactional
  public void fetchGovActionMetadata() {
    long startTime = System.currentTimeMillis();
    log.info("Start fetching gov action metadata");
    DataCheckpoint currentCheckpoint =
        dataCheckpointRepository
            .findFirstByType(DataCheckpointType.GOV_ACTION_DATA)
            .orElse(
                DataCheckpoint.builder()
                    .slotNo(0L)
                    .type(DataCheckpointType.GOV_ACTION_DATA)
                    .build());

    long currentSlotNo = govActionProposalRepository.maxSlotNo().orElse(0L);
    List<Anchor> anchorList =
        govActionProposalRepository.getAnchorInfoBySlotRange(
            currentCheckpoint.getSlotNo(), currentSlotNo);

    if (Collections.isEmpty(anchorList)) {
      log.info(
          "No anchor data to fetch from slot {} to slot {}",
          currentCheckpoint.getSlotNo(),
          currentSlotNo);
    } else {
      fetchingDataService.initOffChainListData();
      fetchingDataService.crawlOffChainAnchors(anchorList);
      List<OffChainVoteFetchError> offChainVoteFetchErrors =
          fetchingDataService.getOffChainAnchorsFetchError();
      List<OffChainVoteGovActionData> offChainVoteGovActionDataList =
          fetchingDataService.getOffChainAnchorsFetchSuccess();

      storingDataService.insertFetchSuccessData(offChainVoteGovActionDataList);
      storingDataService.insertFetchFailData(offChainVoteFetchErrors);
    }
    currentCheckpoint.setSlotNo(currentSlotNo);
    currentCheckpoint.setUpdateTime(Timestamp.valueOf(LocalDateTime.now()));
    dataCheckpointRepository.save(currentCheckpoint);
    log.info(
        "End fetching gov action metadata, taken time: {} ms",
        System.currentTimeMillis() - startTime);
  }

  /** Retry fetch gov action metadata. */
  @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
  //  @Scheduled(fixedRateString = "${jobs.gov-action-metadata.fixed-delay}")
  @Transactional
  public void retryFetchGovActionMetadata() {
    long startTime = System.currentTimeMillis();
    log.info("Start retry fetching gov action metadata");
    retryDataService.initOffChainListData();
    List<Anchor> anchorList = govActionProposalRepository.getAnchorInfoByRetryCount(retryCount);

    retryDataService.crawlOffChainAnchors(anchorList);
    List<OffChainVoteFetchError> offChainVoteFetchErrors =
        retryDataService.getOffChainAnchorsFetchError();
    List<OffChainVoteGovActionData> offChainVoteGovActionDataList =
        retryDataService.getOffChainAnchorsFetchSuccess();

    storingDataService.insertFetchSuccessData(offChainVoteGovActionDataList);
    storingDataService.insertFetchFailData(offChainVoteFetchErrors);
    log.info(
        "End retry fetching gov action metadata, taken time: {} ms",
        System.currentTimeMillis() - startTime);
  }
}
