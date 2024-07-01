package org.cardanofoundation.job.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import org.apache.logging.log4j.util.Strings;

import org.cardanofoundation.explorer.common.entity.explorer.PoolReportHistory;
import org.cardanofoundation.explorer.common.entity.explorer.StakeKeyReportHistory;
import org.cardanofoundation.job.common.enumeration.EventType;
import org.cardanofoundation.job.dto.report.pool.EpochSize;
import org.cardanofoundation.job.dto.report.pool.InformationReport;
import org.cardanofoundation.job.dto.report.pool.PoolDeregistration;
import org.cardanofoundation.job.dto.report.pool.PoolRegistration;
import org.cardanofoundation.job.dto.report.pool.PoolUpdate;
import org.cardanofoundation.job.dto.report.pool.RewardDistribution;
import org.cardanofoundation.job.dto.report.stake.StakeDelegationFilterResponse;
import org.cardanofoundation.job.dto.report.stake.StakeLifeCycleFilterRequest;
import org.cardanofoundation.job.dto.report.stake.StakeRegistrationLifeCycle;
import org.cardanofoundation.job.dto.report.stake.StakeRewardResponse;
import org.cardanofoundation.job.dto.report.stake.StakeWalletActivityResponse;
import org.cardanofoundation.job.dto.report.stake.StakeWithdrawalFilterResponse;
import org.cardanofoundation.job.util.DataUtil;
import org.cardanofoundation.job.util.report.ExportContent;

@Component
@RequiredArgsConstructor
@Log4j2
public class ReportHistoryServiceAsync {

  private static final String DELEGATION_HISTORY_TITLE = "Delegation History";
  private static final String WITHDRAWAL_HISTORY_TITLE = "Withdrawal History";
  private static final String WALLET_ACTIVITY_TITLE = "ADA Transfer";
  private static final String POOL_SIZE_TITLE = "Pool Size";
  private static final String REGISTRATIONS_TITLE = "Registration";
  private static final String POOL_UPDATE_TITLE = "Pool Update";
  private static final String REWARD_DISTRIBUTION_TITLE = "Reward Distribution";
  private static final String OPERATOR_REWARDS_TITLE = "Operator Rewards";
  private static final String DEREGISTRATION_TITLE = "Deregistration";
  private static final String NOT_AVAILABLE = "Not available";
  private static final String INFORMATION_ON_THE_REPORT_TITLE = "Information on the Report";

  private final StakeKeyLifeCycleService stakeKeyLifeCycleService;

  private final PoolLifecycleService poolLifecycleService;
  private final FetchRewardDataService fetchRewardDataService;

  @Value("${jobs.limit-content}")
  private int limitSize;

  private Pageable defPageableStake;
  private Pageable defPageablePool;

  @PostConstruct
  public void init() {
    defPageableStake = PageRequest.of(0, limitSize, Sort.by("time").descending());
    defPageablePool = PageRequest.of(0, limitSize, Sort.by("id").descending());
  }

  /**
   * Get stake wallet activitys export content
   *
   * @param stakeKey stake key
   * @param condition filter condition
   * @param pageable page request
   * @param subTitle sub title
   * @return export content
   */
  @Async
  public CompletableFuture<ExportContent> exportStakeWalletActivitys(
      String stakeKey, StakeLifeCycleFilterRequest condition, Pageable pageable, String subTitle) {

    List<StakeWalletActivityResponse> walletActivityResponses =
        stakeKeyLifeCycleService.getStakeWalletActivities(stakeKey, pageable, condition);

    return CompletableFuture.completedFuture(
        ExportContent.builder()
            .clazz(StakeWalletActivityResponse.class)
            .headerTitle(WALLET_ACTIVITY_TITLE + subTitle)
            .lstColumn(StakeWalletActivityResponse.buildExportColumn())
            .lstData(walletActivityResponses)
            .build());
  }

  /**
   * Get stake withdrawals export content
   *
   * @param stakeKey stake key
   * @param condition filter condition
   * @return export content
   */
  @Async
  public CompletableFuture<ExportContent> exportStakeRegistrations(
      String stakeKey, StakeLifeCycleFilterRequest condition) {
    List<StakeRegistrationLifeCycle> stakeRegistrations =
        stakeKeyLifeCycleService.getStakeRegistrations(stakeKey, defPageableStake, condition);

    return CompletableFuture.completedFuture(
        ExportContent.builder()
            .clazz(StakeRegistrationLifeCycle.class)
            .headerTitle(REGISTRATIONS_TITLE)
            .lstColumn(StakeRegistrationLifeCycle.buildExportColumn())
            .lstData(stakeRegistrations)
            .build());
  }

  /**
   * Get stake delegations export content
   *
   * @param stakeKey stake key
   * @param condition filter condition
   * @return export content
   */
  @Async
  public CompletableFuture<ExportContent> exportStakeDelegations(
      String stakeKey, StakeLifeCycleFilterRequest condition) {
    List<StakeDelegationFilterResponse> stakeDelegations =
        stakeKeyLifeCycleService.getStakeDelegations(stakeKey, defPageableStake, condition);

    return CompletableFuture.completedFuture(
        ExportContent.builder()
            .clazz(StakeDelegationFilterResponse.class)
            .headerTitle(DELEGATION_HISTORY_TITLE)
            .lstColumn(StakeDelegationFilterResponse.buildExportColumn())
            .lstData(stakeDelegations)
            .build());
  }

  /**
   * Get stake rewards export content
   *
   * @param stakeKey stake key
   * @param condition filter condition
   * @return export content
   */
  @Async
  public CompletableFuture<ExportContent> exportStakeRewards(
      String stakeKey, StakeLifeCycleFilterRequest condition) {
    ExportContent exportContent =
        ExportContent.builder()
            .clazz(StakeRewardResponse.class)
            .headerTitle(REWARD_DISTRIBUTION_TITLE)
            .lstColumn(StakeRewardResponse.buildExportColumn())
            .build();

    if (Boolean.TRUE.equals(fetchRewardDataService.isKoiOs())) {
      exportContent.setLstData(
          stakeKeyLifeCycleService.getStakeRewards(stakeKey, defPageablePool, condition));
    } else {
      exportContent.setLstData(Collections.emptyList());
      exportContent.setSimpleMessage(NOT_AVAILABLE);
    }

    return CompletableFuture.completedFuture(exportContent);
  }

  /**
   * Get stake withdrawals export content
   *
   * @param stakeKey stake key
   * @param stakeLifeCycleFilterRequest filter condition
   * @return export content
   */
  @Async
  public CompletableFuture<ExportContent> exportStakeWithdrawals(
      String stakeKey, StakeLifeCycleFilterRequest stakeLifeCycleFilterRequest) {
    List<StakeWithdrawalFilterResponse> stakeWithdrawals =
        stakeKeyLifeCycleService.getStakeWithdrawals(
            stakeKey, defPageableStake, stakeLifeCycleFilterRequest);
    return CompletableFuture.completedFuture(
        ExportContent.builder()
            .clazz(StakeWithdrawalFilterResponse.class)
            .headerTitle(WITHDRAWAL_HISTORY_TITLE)
            .lstColumn(StakeWithdrawalFilterResponse.buildExportColumn())
            .lstData(stakeWithdrawals)
            .build());
  }

  /**
   * Get stake de-registrations export content
   *
   * @param stakeKey stake key
   * @param stakeLifeCycleFilterRequest filter condition
   * @return export content
   */
  @Async
  public CompletableFuture<ExportContent> exportStakeDeregistrations(
      String stakeKey, StakeLifeCycleFilterRequest stakeLifeCycleFilterRequest) {
    List<StakeRegistrationLifeCycle> stakeDeRegistrations =
        stakeKeyLifeCycleService.getStakeDeRegistrations(
            stakeKey, defPageableStake, stakeLifeCycleFilterRequest);

    return CompletableFuture.completedFuture(
        ExportContent.builder()
            .clazz(StakeRegistrationLifeCycle.class)
            .headerTitle(DEREGISTRATION_TITLE)
            .lstColumn(StakeRegistrationLifeCycle.buildExportColumn())
            .lstData(stakeDeRegistrations)
            .build());
  }

  /**
   * Get pool size export content
   *
   * @param poolReport pool report
   * @return export content
   */
  @Async
  public CompletableFuture<ExportContent> exportEpochSize(PoolReportHistory poolReport) {
    ExportContent exportContent =
        ExportContent.builder()
            .clazz(EpochSize.class)
            .headerTitle(POOL_SIZE_TITLE)
            .lstColumn(EpochSize.buildExportColumn())
            .build();

    if (Boolean.TRUE.equals(fetchRewardDataService.isKoiOs())) {
      Pageable epochSizePage = PageRequest.of(0, limitSize, Sort.by("epochNo").descending());
      exportContent.setLstData(poolLifecycleService.getPoolSizes(poolReport, epochSizePage));
    } else {
      exportContent.setLstData(Collections.emptyList());
      exportContent.setSimpleMessage(NOT_AVAILABLE);
    }

    return CompletableFuture.completedFuture(exportContent);
  }

  /**
   * Get pool registration export content
   *
   * @param poolReport pool report
   * @return export content
   */
  @Async
  public CompletableFuture<ExportContent> exportPoolRegistration(PoolReportHistory poolReport) {
    List<PoolRegistration> poolRegistrations =
        poolLifecycleService.registrationList(poolReport.getPoolView(), defPageablePool).stream()
            .map(PoolRegistration::toDomain)
            .collect(Collectors.toList());

    return CompletableFuture.completedFuture(
        ExportContent.builder()
            .clazz(PoolRegistration.class)
            .headerTitle(REGISTRATIONS_TITLE)
            .lstColumn(PoolRegistration.buildExportColumn())
            .lstData(poolRegistrations)
            .build());
  }

  /**
   * Get pool update export content
   *
   * @param poolReport pool report
   * @return export content
   */
  @Async
  public CompletableFuture<ExportContent> exportPoolUpdate(PoolReportHistory poolReport) {
    List<PoolUpdate> poolRegistrations =
        poolLifecycleService.poolUpdateList(poolReport.getPoolView(), defPageablePool).stream()
            .map(PoolUpdate::toDomain)
            .collect(Collectors.toList());

    return CompletableFuture.completedFuture(
        ExportContent.builder()
            .clazz(PoolUpdate.class)
            .headerTitle(POOL_UPDATE_TITLE)
            .lstColumn(PoolUpdate.buildExportColumn())
            .lstData(poolRegistrations)
            .build());
  }

  /**
   * Get pool rewards distribution export content
   *
   * @param poolReport pool report
   * @return export content
   */
  @Async
  public CompletableFuture<ExportContent> exportRewardsDistribution(PoolReportHistory poolReport) {
    ExportContent exportContent =
        ExportContent.builder()
            .clazz(RewardDistribution.class)
            .headerTitle(OPERATOR_REWARDS_TITLE)
            .lstColumn(RewardDistribution.buildExportColumn())
            .build();

    if (Boolean.TRUE.equals(fetchRewardDataService.isKoiOs())) {
      exportContent.setLstData(
          poolLifecycleService.listReward(poolReport, defPageablePool).stream()
              .map(RewardDistribution::toDomain)
              .collect(Collectors.toList()));
    } else {
      exportContent.setLstData(Collections.emptyList());
      exportContent.setSimpleMessage(NOT_AVAILABLE);
    }

    return CompletableFuture.completedFuture(exportContent);
  }

  /**
   * Get pool de-registration export content
   *
   * @param poolReport pool report
   * @return export content
   */
  @Async
  public CompletableFuture<ExportContent> exportPoolDeregistration(PoolReportHistory poolReport) {
    List<PoolDeregistration> poolRegistrations =
        poolLifecycleService.deRegistration(poolReport.getPoolView(), defPageablePool).stream()
            .map(PoolDeregistration::toDomain)
            .collect(Collectors.toList());

    return CompletableFuture.completedFuture(
        ExportContent.builder()
            .clazz(PoolDeregistration.class)
            .headerTitle(DEREGISTRATION_TITLE)
            .lstColumn(PoolDeregistration.buildExportColumn())
            .lstData(poolRegistrations)
            .build());
  }

  @Async
  public CompletableFuture<ExportContent> exportInformationOnTheReport(
      PoolReportHistory poolReportHistory) {
    InformationReport informationReport = buildPoolReport(poolReportHistory);

    List<InformationReport> list = new ArrayList<>(List.of(informationReport));
    return CompletableFuture.completedFuture(
        ExportContent.builder()
            .clazz(InformationReport.class)
            .headerTitle(INFORMATION_ON_THE_REPORT_TITLE)
            .lstColumn(InformationReport.buildExportColumn(true))
            .lstData(list)
            .build());
  }

  @Async
  public CompletableFuture<ExportContent> exportInformationOnTheReport(
      StakeKeyReportHistory stakeKeyReportHistory) {
    InformationReport informationReport = buildStakeReport(stakeKeyReportHistory);

    List<InformationReport> list = new ArrayList<>(List.of(informationReport));
    return CompletableFuture.completedFuture(
        ExportContent.builder()
            .clazz(InformationReport.class)
            .headerTitle(INFORMATION_ON_THE_REPORT_TITLE)
            .lstColumn(InformationReport.buildExportColumn(false))
            .lstData(list)
            .build());
  }

  private InformationReport buildPoolReport(PoolReportHistory poolReportHistory) {
    Integer epochBegin = poolReportHistory.getBeginEpoch();
    Integer epochEnd = poolReportHistory.getEndEpoch();

    List<String> events = new ArrayList<>();

    if (poolReportHistory.getEventPoolUpdate()) {
      events.add(EventType.POOL_UPDATE.getValue());
    }
    if (poolReportHistory.getEventDeregistration()) {
      events.add(EventType.DE_REGISTRATION.getValue());
    }
    if (poolReportHistory.getEventRegistration()) {
      events.add(EventType.REGISTRATION.getValue());
    }
    if (poolReportHistory.getEventReward()) {
      events.add(EventType.REWARDS.getValue());
    }

    String epochRange =
        String.join(
            " - ",
            epochBegin == null ? "N/A" : Strings.concat("Epoch ", epochBegin.toString()),
            epochEnd == null ? "N/A" : Strings.concat("Epoch ", epochEnd.toString()));

    String eventsString = events.isEmpty() ? null : String.join(", ", events);
    return InformationReport.builder()
        .createdAt(poolReportHistory.getReportHistory().getCreatedAt())
        .reportType("Pool Report")
        .poolId(poolReportHistory.getPoolView())
        .reportName(poolReportHistory.getReportHistory().getReportName())
        .epochRange(epochRange)
        .dateTimeFormat(poolReportHistory.getReportHistory().getDateFormat())
        .events(eventsString)
        .isPoolSize(Boolean.TRUE.equals(poolReportHistory.getIsPoolSize()) ? "Yes" : "No")
        .build();
  }

  private InformationReport buildStakeReport(StakeKeyReportHistory stakeKeyReportHistory) {

    Timestamp fromDate = stakeKeyReportHistory.getFromDate();
    Timestamp toDate = stakeKeyReportHistory.getToDate();

    String timePattern = stakeKeyReportHistory.getReportHistory().getTimePattern();
    Long zoneOffset = stakeKeyReportHistory.getReportHistory().getZoneOffset();
    String dateFormat = stakeKeyReportHistory.getReportHistory().getDateFormat();

    String datePattern = timePattern.substring(0, timePattern.indexOf(','));

    String dateRange =
        String.join(
            " - ",
            fromDate == null ? "N/A" : DataUtil.dateToString(fromDate, zoneOffset, datePattern),
            toDate == null ? "N/A" : DataUtil.dateToString(toDate, zoneOffset, datePattern));

    List<String> stakeEvents = new ArrayList<>();

    if (stakeKeyReportHistory.getEventRegistration()) {
      stakeEvents.add(EventType.REGISTRATION.getValue());
    }
    if (stakeKeyReportHistory.getEventDelegation()) {
      stakeEvents.add(EventType.DELEGATION.getValue());
    }
    if (stakeKeyReportHistory.getEventRewards()) {
      stakeEvents.add(EventType.REWARDS.getValue());
    }
    if (stakeKeyReportHistory.getEventWithdrawal()) {
      stakeEvents.add(EventType.WITHDRAWAL.getValue());
    }
    if (stakeKeyReportHistory.getEventDeregistration()) {
      stakeEvents.add(EventType.DE_REGISTRATION.getValue());
    }

    String eventsString = stakeEvents.isEmpty() ? null : String.join(", ", stakeEvents);

    return InformationReport.builder()
        .createdAt(stakeKeyReportHistory.getReportHistory().getCreatedAt())
        .reportType("Stake Address Report")
        .stakeAddress(stakeKeyReportHistory.getStakeKey())
        .reportName(stakeKeyReportHistory.getReportHistory().getReportName())
        .dateRange(dateRange)
        .dateTimeFormat(dateFormat)
        .events(eventsString)
        .isADATransfers(
            Boolean.TRUE.equals(stakeKeyReportHistory.getIsADATransfer()) ? "Yes" : "No")
        .build();
  }
}
