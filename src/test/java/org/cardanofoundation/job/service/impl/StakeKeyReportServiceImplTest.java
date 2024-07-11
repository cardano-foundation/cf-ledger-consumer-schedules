package org.cardanofoundation.job.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.conversions.ClasspathConversionsFactory;
import org.cardanofoundation.conversions.domain.NetworkType;
import org.cardanofoundation.explorer.common.entity.enumeration.ReportStatus;
import org.cardanofoundation.explorer.common.entity.enumeration.ReportType;
import org.cardanofoundation.explorer.common.entity.explorer.ReportHistory;
import org.cardanofoundation.explorer.common.entity.explorer.StakeKeyReportHistory;
import org.cardanofoundation.job.dto.report.stake.StakeLifeCycleFilterRequest;
import org.cardanofoundation.job.repository.explorer.StakeKeyReportHistoryRepository;
import org.cardanofoundation.job.repository.ledgersync.AddressTxAmountRepository;
import org.cardanofoundation.job.service.ReportHistoryServiceAsync;
import org.cardanofoundation.job.util.report.ExcelHelper;
import org.cardanofoundation.job.util.report.ExportContent;

@ExtendWith(MockitoExtension.class)
class StakeKeyReportServiceImplTest {

  @Mock ReportHistoryServiceAsync reportHistoryServiceAsync;

  @Mock ExcelHelper excelHelper;

  @Mock StorageReportServiceImpl storageService;

  @Mock AddressTxAmountRepository addressTxAmountRepository;

  @InjectMocks StakeKeyReportServiceImpl stakeKeyReportService;

  @Mock StakeKeyReportHistoryRepository stakeKeyReportHistoryRepository;

  CardanoConverters cardanoConverters =
      ClasspathConversionsFactory.createConverters(NetworkType.MAINNET);

  @BeforeEach
  public void setUp() {
    ReflectionTestUtils.setField(stakeKeyReportService, "limitSize", 1000000);
    ReflectionTestUtils.setField(stakeKeyReportService, "cardanoConverters", cardanoConverters);
  }

  @Test
  void exportStakeKeyReport_shouldThrowExceptionWhenPersistFileToStorageFail() {
    Timestamp fromDate = Timestamp.valueOf("2023-01-01 00:00:00");
    Timestamp toDate =
        Timestamp.from(
            LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toInstant(ZoneOffset.UTC));
    StakeLifeCycleFilterRequest condition =
        StakeLifeCycleFilterRequest.builder().fromDate(fromDate).toDate(toDate).build();
    String stakeKey = "stake1u98ujxfgzdm8yh6qsaar54nmmr50484t4ytphxjex3zxh7g4tuwna";
    String username = "username";
    ReportHistory reportHistory =
        ReportHistory.builder()
            .username(username)
            .reportName("reportName")
            .status(ReportStatus.IN_PROGRESS)
            .type(ReportType.STAKE_KEY)
            .build();
    StakeKeyReportHistory stakeKeyReportHistory =
        spy(
            StakeKeyReportHistory.builder()
                .stakeKey(stakeKey)
                .fromDate(fromDate)
                .toDate(toDate)
                .isADATransfer(Boolean.TRUE)
                .eventRegistration(Boolean.TRUE)
                .eventDeregistration(Boolean.TRUE)
                .eventWithdrawal(Boolean.TRUE)
                .eventRewards(Boolean.TRUE)
                .eventDelegation(Boolean.TRUE)
                .reportHistory(reportHistory)
                .build());

    when(reportHistoryServiceAsync.exportInformationOnTheReport(
            any(), anyLong(), anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));

    when(reportHistoryServiceAsync.exportStakeRegistrations(stakeKey, condition))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportStakeDelegations(stakeKey, condition))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportStakeRewards(stakeKey, condition))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportStakeWithdrawals(stakeKey, condition))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportStakeDeregistrations(stakeKey, condition))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));

    var from = cardanoConverters.time().toSlot(condition.getFromDate().toLocalDateTime());
    var to = cardanoConverters.time().toSlot(condition.getToDate().toLocalDateTime());

    when(addressTxAmountRepository.getCountTxByStakeInSlotRange(stakeKey, from, to))
        .thenReturn(2000000L);

    when(reportHistoryServiceAsync.exportStakeWalletActivitys(
            anyString(), any(StakeLifeCycleFilterRequest.class), any(Pageable.class), anyString()))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(excelHelper.writeContent(anyList(), anyLong(), anyString()))
        .thenReturn(new ByteArrayInputStream(new byte[0]));

    doThrow(new RuntimeException()).when(storageService).uploadFile(any(), anyString());
    Assertions.assertThrows(
        Exception.class,
        () ->
            stakeKeyReportService.exportStakeKeyReport(
                stakeKeyReportHistory, 0L, "MM/dd/yyyy HH:mm:ss", "MM/DD/YYYY (UTC)"));
    Assertions.assertEquals(
        ReportStatus.FAILED, stakeKeyReportHistory.getReportHistory().getStatus());
  }

  @Test
  void exportStakeKeyReport_shouldSuccess() {
    Timestamp fromDate = Timestamp.valueOf("2023-01-01 00:00:00");
    Timestamp toDate =
        Timestamp.from(
            LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toInstant(ZoneOffset.UTC));
    StakeLifeCycleFilterRequest condition =
        StakeLifeCycleFilterRequest.builder().fromDate(fromDate).toDate(toDate).build();
    String stakeKey = "stake1u98ujxfgzdm8yh6qsaar54nmmr50484t4ytphxjex3zxh7g4tuwna";
    String username = "username";
    ReportHistory reportHistory =
        ReportHistory.builder()
            .username(username)
            .reportName("reportName")
            .status(ReportStatus.IN_PROGRESS)
            .type(ReportType.STAKE_KEY)
            .build();
    StakeKeyReportHistory stakeKeyReportHistory =
        spy(
            StakeKeyReportHistory.builder()
                .stakeKey(stakeKey)
                .fromDate(fromDate)
                .toDate(toDate)
                .isADATransfer(Boolean.TRUE)
                .eventRegistration(Boolean.TRUE)
                .eventDeregistration(Boolean.TRUE)
                .eventWithdrawal(Boolean.TRUE)
                .eventRewards(Boolean.TRUE)
                .eventDelegation(Boolean.TRUE)
                .reportHistory(reportHistory)
                .build());

    when(reportHistoryServiceAsync.exportInformationOnTheReport(
            any(), anyLong(), anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));

    when(reportHistoryServiceAsync.exportStakeRegistrations(stakeKey, condition))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportStakeDelegations(stakeKey, condition))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportStakeRewards(stakeKey, condition))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportStakeWithdrawals(stakeKey, condition))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(reportHistoryServiceAsync.exportStakeDeregistrations(stakeKey, condition))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));

    var from = cardanoConverters.time().toSlot(condition.getFromDate().toLocalDateTime());
    var to = cardanoConverters.time().toSlot(condition.getToDate().toLocalDateTime());

    when(addressTxAmountRepository.getCountTxByStakeInSlotRange(stakeKey, from, to))
        .thenReturn(2000000L);

    when(reportHistoryServiceAsync.exportStakeWalletActivitys(
            anyString(), any(StakeLifeCycleFilterRequest.class), any(Pageable.class), anyString()))
        .thenReturn(CompletableFuture.completedFuture(ExportContent.builder().build()));
    when(excelHelper.writeContent(anyList(), anyLong(), anyString()))
        .thenReturn(new ByteArrayInputStream(new byte[0]));
    doNothing().when(storageService).uploadFile(any(), anyString());
    when(stakeKeyReportHistoryRepository.save(any(StakeKeyReportHistory.class)))
        .thenReturn(new StakeKeyReportHistory());

    Assertions.assertDoesNotThrow(
        () ->
            stakeKeyReportService.exportStakeKeyReport(
                stakeKeyReportHistory, 0L, "MM/dd/yyyy HH:mm:ss", "MM/DD/YYYY (UTC)"));
    Assertions.assertEquals(
        ReportStatus.GENERATED, stakeKeyReportHistory.getReportHistory().getStatus());
  }
}
