package org.cardanofoundation.job.service.impl;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import org.cardanofoundation.explorer.common.entity.compositeKey.OffChainVoteFetchErrorId;
import org.cardanofoundation.explorer.common.entity.compositeKey.OffChainVoteGovActionDataId;
import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteFetchError;
import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteGovActionData;
import org.cardanofoundation.job.repository.ledgersync.OffChainVoteFetchErrorRepository;
import org.cardanofoundation.job.repository.ledgersync.OffChainVoteGovActionRepository;
import org.cardanofoundation.job.service.OffChainVoteStoringService;

@Service
@Slf4j
@RequiredArgsConstructor
public class OffChainVoteGovActionStoringServiceImpl
    extends OffChainVoteStoringService<OffChainVoteGovActionData, OffChainVoteFetchError> {

  private final OffChainVoteFetchErrorRepository offChainVoteFetchErrorRepository;
  private final OffChainVoteGovActionRepository offChainVoteGovActionRepository;

  @Override
  public void insertFetchSuccessData(Collection<OffChainVoteGovActionData> offChainAnchorData) {
    Set<OffChainVoteGovActionDataId> offChainVoteGovActionDataIds =
        offChainAnchorData.stream()
            .map(OffChainVoteGovActionData::getId)
            .collect(Collectors.toSet());

    Set<OffChainVoteGovActionData> existingOffChainVoteGovActionData =
        new HashSet<>(offChainVoteGovActionRepository.findAllById(offChainVoteGovActionDataIds));

    List<OffChainVoteGovActionData> offChainVoteGovActionDataToSave =
        offChainAnchorData.stream()
            .filter(
                offChainVoteGovActionData ->
                    !existingOffChainVoteGovActionData.contains(offChainVoteGovActionData))
            .collect(Collectors.toList());

    offChainVoteGovActionRepository.saveAll(offChainVoteGovActionDataToSave);
  }

  @Override
  public void insertFetchFailData(Collection<OffChainVoteFetchError> offChainFetchErrorData) {
    Timestamp currentTime = new Timestamp(System.currentTimeMillis());

    // Remove all duplicates OffChainVoteFetchErrorId
    offChainFetchErrorData =
        offChainFetchErrorData.stream()
            .filter(distinctByKey(OffChainVoteFetchError::getId))
            .collect(Collectors.toList());

    Set<OffChainVoteFetchErrorId> offChainVoteGovActionDataIds =
        offChainFetchErrorData.stream()
            .map(OffChainVoteFetchError::getId)
            .collect(Collectors.toSet());

    Map<OffChainVoteFetchErrorId, OffChainVoteFetchError> existingOffChainVoteFetchErrorMap =
        offChainVoteFetchErrorRepository.findAllById(offChainVoteGovActionDataIds).stream()
            .collect(Collectors.toMap(OffChainVoteFetchError::getId, Function.identity()));

    offChainFetchErrorData.forEach(
        offChainVoteFetchError -> {
          OffChainVoteFetchError existingOffChainVoteFetchError =
              existingOffChainVoteFetchErrorMap.get(offChainVoteFetchError.getId());
          if (existingOffChainVoteFetchError != null) {
            offChainVoteFetchError.setRetryCount(
                existingOffChainVoteFetchError.getRetryCount() + 1);
            offChainVoteFetchError.setFetchTime(currentTime);
          } else {
            offChainVoteFetchError.setRetryCount(1);
            offChainVoteFetchError.setFetchTime(currentTime);
          }
        });

    offChainVoteFetchErrorRepository.saveAll(offChainFetchErrorData);
  }

  public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }
}
