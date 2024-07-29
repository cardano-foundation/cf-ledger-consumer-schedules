package org.cardanofoundation.job.service.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteFetchError;
import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteGovActionData;
import org.cardanofoundation.job.dto.govActionMetaData.Anchor;
import org.cardanofoundation.job.dto.govActionMetaData.OffChainGovActionData;
import org.cardanofoundation.job.repository.ledgersync.OffChainVoteFetchErrorRepository;
import org.cardanofoundation.job.repository.ledgersync.OffChainVoteGovActionRepository;
import org.cardanofoundation.job.service.OffChainVoteGovActionDataStoringService;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OffChainVoteGovActionStoringServiceImpl
    implements OffChainVoteGovActionDataStoringService {

  private final OffChainVoteFetchErrorRepository offChainVoteFetchErrorRepository;
  private final OffChainVoteGovActionRepository offChainVoteGovActionRepository;

  @Override
  @Transactional
  public void insertData(Queue<OffChainGovActionData> offChainVoteGovActionData) {
    Queue<OffChainVoteGovActionData> fetchSuccess = new ConcurrentLinkedQueue<>();
    Queue<OffChainVoteFetchError> fetchFail = new ConcurrentLinkedQueue<>();
    offChainVoteGovActionData.parallelStream()
        .forEach(
            offChainGovActionData -> {
              if (offChainGovActionData != null) {
                if (offChainGovActionData.isFetchSuccess() && offChainGovActionData.isValid()) {
                  OffChainVoteGovActionData save =
                      OffChainVoteGovActionData.builder()
                          .anchorUrl(offChainGovActionData.getAnchorUrl())
                          .anchorHash(offChainGovActionData.getAnchorHash())
                          .title(offChainGovActionData.getTitle())
                          .abstractData(offChainGovActionData.getAbstractContent())
                          .motivation(offChainGovActionData.getMotivation())
                          .rationale(offChainGovActionData.getRationale())
                          .rawData(offChainGovActionData.getRawData())
                          .build();
                  fetchSuccess.add(save);
                } else if (!offChainGovActionData.isFetchSuccess()) {
                  OffChainVoteFetchError save =
                      OffChainVoteFetchError.builder()
                          .anchorUrl(offChainGovActionData.getAnchorUrl())
                          .anchorHash(offChainGovActionData.getAnchorHash())
                          .fetchError(offChainGovActionData.getFetchFailReason())
                          .fetchTime(Timestamp.from(Instant.now()))
                          .retryCount(1)
                          .build();
                  fetchFail.add(save);
                }
              }
            });
    offChainVoteGovActionRepository.saveAll(fetchSuccess);
    offChainVoteFetchErrorRepository.saveAll(fetchFail);
  }

  @Override
  @Transactional
  public void insertFetchRetryData(
      List<OffChainVoteFetchError> offChainVoteFetchError,
      Queue<OffChainGovActionData> offChainGovActionDataListRetry) {
    List<OffChainVoteGovActionData> fetchSuccess = new ArrayList<>();
    Map<Anchor, OffChainGovActionData> offChainGovActionDataListRetryMap =
        offChainGovActionDataListRetry.stream()
            .collect(
                Collectors.toMap(
                    (offChainGovActionData) -> {
                      Anchor anchor = new Anchor();
                      anchor.setAnchorUrl(offChainGovActionData.getAnchorUrl());
                      anchor.setAnchorHash(offChainGovActionData.getAnchorHash());
                      return anchor;
                    },
                    Function.identity()));

    offChainVoteFetchError.forEach(
        (fetchError) -> {
          Anchor anchor =
              Anchor.builder()
                  .anchorHash(fetchError.getAnchorHash())
                  .anchorUrl(fetchError.getAnchorUrl())
                  .build();
          OffChainGovActionData offChainGovActionData =
              offChainGovActionDataListRetryMap.get(anchor);
          if (offChainGovActionData != null) {
            if (offChainGovActionData.isFetchSuccess() && offChainGovActionData.isValid()) {
              OffChainVoteGovActionData save =
                  OffChainVoteGovActionData.builder()
                      .anchorUrl(offChainGovActionData.getAnchorUrl())
                      .anchorHash(offChainGovActionData.getAnchorHash())
                      .rawData(offChainGovActionData.getRawData())
                      .build();
              fetchSuccess.add(save);
            } else if (!offChainGovActionData.isFetchSuccess()) {
              fetchError.setFetchError(offChainGovActionData.getFetchFailReason());
            }
            fetchError.setRetryCount(fetchError.getRetryCount() + 1);
            fetchError.setFetchTime(Timestamp.from(Instant.now()));
          }
        });
    offChainVoteGovActionRepository.saveAll(fetchSuccess);
    offChainVoteFetchErrorRepository.saveAll(offChainVoteFetchError);
  }
}
