package org.cardanofoundation.job.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.explorer.common.entity.compositeKey.OffChainVoteFetchErrorId;
import org.cardanofoundation.explorer.common.entity.compositeKey.OffChainVoteGovActionDataId;
import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteFetchError;
import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteGovActionData;
import org.cardanofoundation.job.repository.ledgersync.OffChainVoteFetchErrorRepository;
import org.cardanofoundation.job.repository.ledgersync.OffChainVoteGovActionRepository;

@ExtendWith(MockitoExtension.class)
class OffChainVoteGovActionStoringServiceImplTest {

  @Mock private OffChainVoteFetchErrorRepository offChainVoteFetchErrorRepository;

  @Mock private OffChainVoteGovActionRepository offChainVoteGovActionRepository;

  @Captor
  private ArgumentCaptor<List<OffChainVoteGovActionData>> offChainVoteGovActionDataLstCaptor;

  @Captor private ArgumentCaptor<List<OffChainVoteFetchError>> offChainFetchErrorDataLstCaptor;

  @InjectMocks private OffChainVoteGovActionStoringServiceImpl offChainVoteGovActionStoringService;

  @Test
  @DisplayName("Should insert fetch success data when no existing data")
  void insertFetchSuccessData_noExistingData() {
    // preparing
    OffChainVoteGovActionData offChainVoteGovActionData1 =
        OffChainVoteGovActionData.builder()
            .id(
                OffChainVoteGovActionDataId.builder()
                    .anchorHash("anchorHash1")
                    .anchorUrl("https://anchor1.com")
                    .build())
            .build();

    OffChainVoteGovActionData offChainVoteGovActionData2 =
        OffChainVoteGovActionData.builder()
            .id(
                OffChainVoteGovActionDataId.builder()
                    .anchorHash("anchorHash2")
                    .anchorUrl("https://anchor2.com")
                    .build())
            .build();

    List<OffChainVoteGovActionData> offChainAnchorDataLst =
        List.of(offChainVoteGovActionData1, offChainVoteGovActionData2);

    // when find existing data should return empty list
    when(offChainVoteGovActionRepository.findAllById(
            Set.of(offChainVoteGovActionData1.getId(), offChainVoteGovActionData2.getId())))
        .thenReturn(List.of());

    // testing
    offChainVoteGovActionStoringService.insertFetchSuccessData(offChainAnchorDataLst);

    // verifying

    // verify insert data
    verify(offChainVoteGovActionRepository).saveAll(offChainVoteGovActionDataLstCaptor.capture());
    offChainVoteGovActionDataLstCaptor
        .getValue()
        .forEach(
            offChainVoteGovActionData ->
                assertTrue(offChainAnchorDataLst.contains(offChainVoteGovActionData)));
  }

  @Test
  @DisplayName("Should insert fetch success data when existing data")
  void insertFetchSuccessData_whenHasExistingData() {
    // preparing
    OffChainVoteGovActionData offChainVoteGovActionData1 =
        OffChainVoteGovActionData.builder()
            .id(
                OffChainVoteGovActionDataId.builder()
                    .anchorHash("anchorHash1")
                    .anchorUrl("https://anchor1.com")
                    .build())
            .build();

    OffChainVoteGovActionData offChainVoteGovActionData2 =
        OffChainVoteGovActionData.builder()
            .id(
                OffChainVoteGovActionDataId.builder()
                    .anchorHash("anchorHash2")
                    .anchorUrl("https://anchor2.com")
                    .build())
            .build();

    List<OffChainVoteGovActionData> offChainAnchorDataLst =
        List.of(offChainVoteGovActionData1, offChainVoteGovActionData2);

    // when find existing data should return offChainVoteGovActionData1
    when(offChainVoteGovActionRepository.findAllById(
            Set.of(offChainVoteGovActionData1.getId(), offChainVoteGovActionData2.getId())))
        .thenReturn(List.of(offChainVoteGovActionData1));

    // executing
    offChainVoteGovActionStoringService.insertFetchSuccessData(offChainAnchorDataLst);

    // verifying
    verify(offChainVoteGovActionRepository).saveAll(offChainVoteGovActionDataLstCaptor.capture());
    // offChainVoteGovActionData1 should not be inserted
    assertFalse(offChainVoteGovActionDataLstCaptor.getValue().contains(offChainVoteGovActionData1));
    // offChainVoteGovActionData2 should be inserted
    assertTrue(offChainVoteGovActionDataLstCaptor.getValue().contains(offChainVoteGovActionData2));
  }

  @Test
  @DisplayName("Should insert fetch fail data when no existing data (all retry_count should be 1)")
  void insertFetchFailData_whenNoExistingData() {
    // preparing
    OffChainVoteFetchError offChainVoteFetchError1 =
        OffChainVoteFetchError.builder()
            .id(
                OffChainVoteFetchErrorId.builder()
                    .anchorHash("anchorHash1")
                    .anchorUrl("https://anchor1.com")
                    .build())
            .build();

    OffChainVoteFetchError offChainVoteFetchError2 =
        OffChainVoteFetchError.builder()
            .id(
                OffChainVoteFetchErrorId.builder()
                    .anchorHash("anchorHash2")
                    .anchorUrl("https://anchor2.com")
                    .build())
            .build();

    OffChainVoteFetchError offChainVoteFetchError1Duplicate =
        OffChainVoteFetchError.builder()
            .id(
                OffChainVoteFetchErrorId.builder()
                    .anchorHash("anchorHash1")
                    .anchorUrl("https://anchor1.com")
                    .build())
            .build();

    List<OffChainVoteFetchError> offChainFetchErrorDataLst =
        List.of(offChainVoteFetchError1, offChainVoteFetchError2, offChainVoteFetchError1Duplicate);

    // when find existing data should return empty list
    when(offChainVoteFetchErrorRepository.findAllById(
            Set.of(offChainVoteFetchError1.getId(), offChainVoteFetchError2.getId())))
        .thenReturn(List.of());

    // executing
    offChainVoteGovActionStoringService.insertFetchFailData(offChainFetchErrorDataLst);

    // verifying
    verify(offChainVoteFetchErrorRepository).saveAll(offChainFetchErrorDataLstCaptor.capture());

    assertEquals(2, offChainFetchErrorDataLstCaptor.getValue().size());
    offChainFetchErrorDataLstCaptor
        .getValue()
        .forEach(
            offChainVoteFetchError -> {
              assertEquals(1, offChainVoteFetchError.getRetryCount());
            });
  }

  @Test
  @DisplayName("Should insert fetch fail data when existing data")
  void insertFetchFailData_whenHasExistingData() {
    // preparing
    OffChainVoteFetchError offChainVoteFetchError1 =
        OffChainVoteFetchError.builder()
            .id(
                OffChainVoteFetchErrorId.builder()
                    .anchorHash("anchorHash1")
                    .anchorUrl("https://anchor1.com")
                    .build())
            .retryCount(2)
            .build();

    OffChainVoteFetchError offChainVoteFetchError2 =
        OffChainVoteFetchError.builder()
            .id(
                OffChainVoteFetchErrorId.builder()
                    .anchorHash("anchorHash2")
                    .anchorUrl("https://anchor2.com")
                    .build())
            .build();

    List<OffChainVoteFetchError> offChainFetchErrorDataLst =
        List.of(offChainVoteFetchError1, offChainVoteFetchError2);

    // when find existing data should return offChainVoteFetchError1
    when(offChainVoteFetchErrorRepository.findAllById(
            Set.of(offChainVoteFetchError1.getId(), offChainVoteFetchError2.getId())))
        .thenReturn(List.of(offChainVoteFetchError1));

    // executing
    offChainVoteGovActionStoringService.insertFetchFailData(offChainFetchErrorDataLst);

    // verifying
    verify(offChainVoteFetchErrorRepository).saveAll(offChainFetchErrorDataLstCaptor.capture());

    assertEquals(2, offChainFetchErrorDataLstCaptor.getValue().size());
    offChainFetchErrorDataLstCaptor
        .getValue()
        .forEach(
            offChainVoteFetchError -> {
              if (offChainVoteFetchError.getId().equals(offChainVoteFetchError1.getId())) {
                assertEquals(3, offChainVoteFetchError.getRetryCount());
              } else {
                assertEquals(1, offChainVoteFetchError.getRetryCount());
              }
            });
  }
}
