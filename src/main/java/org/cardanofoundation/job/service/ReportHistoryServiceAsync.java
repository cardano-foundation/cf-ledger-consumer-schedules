package org.cardanofoundation.job.service;

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

import org.cardanofoundation.explorer.consumercommon.entity.PoolReportHistory;
import org.cardanofoundation.job.dto.report.pool.EpochSize;
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
import org.cardanofoundation.job.projection.EpochStakeRepository;
import org.cardanofoundation.job.util.report.ExportContent;

@Component
@RequiredArgsConstructor
@Log4j2
public class ReportHistoryServiceAsync {

  private static final String DELEGATION_HISTORY_TITLE = "Delegation History";
  private static final String REWARDS_DISTRIBUTION_TITLE = "Rewards Distribution";
  private static final String WITHDRAWAL_HISTORY_TITLE = "Withdrawal History";
  private static final String WALLET_ACTIVITY_TITLE = "ADA Transfer";
  private static final String POOL_SIZE_TITLE = "Pool Size";
  private static final String REGISTRATIONS_TITLE = "Registrations";
  private static final String POOL_UPDATE_TITLE = "Pool Update";
  private static final String REWARD_DISTRIBUTION_TITLE = "Reward Distribution";
  private static final String DEREGISTRATION_TITLE = "Deregistration";
  private final StakeKeyLifeCycleService stakeKeyLifeCycleService;

  private final EpochStakeRepository epochStakeRepository;
  private final PoolLifecycleService poolLifecycleService;

  @Value("${jobs.limit-content}")
  private int limitSize;
  private Pageable defPageableStake;
  private Pageable defPageablePool;


  @PostConstruct
  public void init() {
    defPageableStake = PageRequest.of(0, limitSize, Sort.by("time").descending());
    defPageablePool = PageRequest.of(0, limitSize, Sort.by("id").descending());
  }


  @Async
  public CompletableFuture<ExportContent> exportStakeWalletActivitys(String stakeKey,
                                                                     StakeLifeCycleFilterRequest condition) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());

    List<StakeWalletActivityResponse> walletActivityResponses = stakeKeyLifeCycleService
        .getStakeWalletActivities(stakeKey, defPageableStake, condition);

    return CompletableFuture.completedFuture(ExportContent.builder()
                                                 .clazz(StakeWalletActivityResponse.class)
                                                 .headerTitle(WALLET_ACTIVITY_TITLE)
                                                 .lstColumn(
                                                     StakeWalletActivityResponse.buildExportColumn())
                                                 .lstData(walletActivityResponses)
                                                 .build());
  }


  @Async
  public CompletableFuture<ExportContent> exportStakeRegistrations(String stakeKey,
                                                                   StakeLifeCycleFilterRequest condition) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    List<StakeRegistrationLifeCycle> stakeRegistrations = stakeKeyLifeCycleService
        .getStakeRegistrations(stakeKey, defPageableStake, condition);

    return CompletableFuture.completedFuture(ExportContent.builder()
                                                 .clazz(StakeRegistrationLifeCycle.class)
                                                 .headerTitle(REGISTRATIONS_TITLE)
                                                 .lstColumn(
                                                     StakeRegistrationLifeCycle.buildExportColumn())
                                                 .lstData(stakeRegistrations)
                                                 .build());
  }

  @Async
  public CompletableFuture<ExportContent> exportStakeDelegations(String stakeKey,
                                                                 StakeLifeCycleFilterRequest condition) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    List<StakeDelegationFilterResponse> stakeDelegations = stakeKeyLifeCycleService
        .getStakeDelegations(stakeKey, defPageableStake, condition);

    return CompletableFuture.completedFuture(ExportContent.builder()
                                                 .clazz(StakeDelegationFilterResponse.class)
                                                 .headerTitle(DELEGATION_HISTORY_TITLE)
                                                 .lstColumn(
                                                     StakeDelegationFilterResponse.buildExportColumn())
                                                 .lstData(stakeDelegations)
                                                 .build());
  }

  @Async
  public CompletableFuture<ExportContent> exportStakeRewards(String stakeKey,
                                                             StakeLifeCycleFilterRequest condition) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    List<StakeRewardResponse> stakeRewards = stakeKeyLifeCycleService.getStakeRewards(
        stakeKey, defPageablePool, condition);

    return CompletableFuture.completedFuture(ExportContent.builder()
                                                 .clazz(StakeRewardResponse.class)
                                                 .headerTitle(REWARDS_DISTRIBUTION_TITLE)
                                                 .lstColumn(StakeRewardResponse.buildExportColumn())
                                                 .lstData(stakeRewards)
                                                 .build());
  }

  @Async
  public CompletableFuture<ExportContent> exportStakeWithdrawals(String stakeKey,
                                                                 StakeLifeCycleFilterRequest stakeLifeCycleFilterRequest) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    List<StakeWithdrawalFilterResponse> stakeWithdrawals = stakeKeyLifeCycleService.getStakeWithdrawals(
        stakeKey, defPageableStake, stakeLifeCycleFilterRequest);
    return CompletableFuture.completedFuture(ExportContent.builder()
                                                 .clazz(StakeWithdrawalFilterResponse.class)
                                                 .headerTitle(WITHDRAWAL_HISTORY_TITLE)
                                                 .lstColumn(
                                                     StakeWithdrawalFilterResponse.buildExportColumn())
                                                 .lstData(stakeWithdrawals)
                                                 .build());
  }

  @Async
  public CompletableFuture<ExportContent> exportStakeDeregistrations(String stakeKey,
                                                                     StakeLifeCycleFilterRequest stakeLifeCycleFilterRequest) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    List<StakeRegistrationLifeCycle> stakeDeRegistrations = stakeKeyLifeCycleService.getStakeDeRegistrations(
        stakeKey, defPageableStake, stakeLifeCycleFilterRequest);

    return CompletableFuture.completedFuture(ExportContent.builder()
                                                 .clazz(StakeRegistrationLifeCycle.class)
                                                 .headerTitle(DEREGISTRATION_TITLE)
                                                 .lstColumn(
                                                     StakeRegistrationLifeCycle.buildExportColumn())
                                                 .lstData(stakeDeRegistrations)
                                                 .build());
  }

  @Async
  public CompletableFuture<ExportContent> exportEpochSize(PoolReportHistory poolReport) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    Pageable epochSizePage = PageRequest.of(0, limitSize, Sort.by("epochNo").descending());
    List<EpochSize> epochSizes = epochStakeRepository
        .getEpochSizeByPoolReport(poolReport.getPoolView(), poolReport.getBeginEpoch(),
                                  poolReport.getEndEpoch(), epochSizePage)
        .getContent()
        .stream()
        .map(EpochSize::toDomain)
        .collect(Collectors.toList());

    return CompletableFuture.completedFuture(ExportContent.builder()
                                                 .clazz(EpochSize.class)
                                                 .headerTitle(POOL_SIZE_TITLE)
                                                 .lstColumn(EpochSize.buildExportColumn())
                                                 .lstData(epochSizes)
                                                 .build());
  }

  @Async
  public CompletableFuture<ExportContent> exportPoolRegistration(PoolReportHistory poolReport) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    List<PoolRegistration> poolRegistrations = poolLifecycleService.registrationList(
            poolReport.getPoolView(), defPageablePool)
        .stream()
        .map(PoolRegistration::toDomain)
        .collect(Collectors.toList());

    return CompletableFuture.completedFuture(ExportContent.builder()
                                                 .clazz(PoolRegistration.class)
                                                 .headerTitle(REGISTRATIONS_TITLE)
                                                 .lstColumn(PoolRegistration.buildExportColumn())
                                                 .lstData(poolRegistrations)
                                                 .build());
  }

  @Async
  public CompletableFuture<ExportContent> exportPoolUpdate(PoolReportHistory poolReport) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    List<PoolUpdate> poolRegistrations = poolLifecycleService.poolUpdateList(
            poolReport.getPoolView(), defPageablePool)
        .stream()
        .map(PoolUpdate::toDomain)
        .collect(Collectors.toList());

    return CompletableFuture.completedFuture(ExportContent.builder()
                                                 .clazz(PoolUpdate.class)
                                                 .headerTitle(POOL_UPDATE_TITLE)
                                                 .lstColumn(PoolUpdate.buildExportColumn())
                                                 .lstData(poolRegistrations)
                                                 .build());
  }

  @Async
  public CompletableFuture<ExportContent> exportRewardsDistribution(PoolReportHistory poolReport) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    List<RewardDistribution> poolRegistrations = poolLifecycleService.listReward(
            poolReport.getPoolView(), defPageablePool)
        .stream()
        .map(RewardDistribution::toDomain)
        .collect(Collectors.toList());

    return CompletableFuture.completedFuture(ExportContent.builder()
                                                 .clazz(RewardDistribution.class)
                                                 .headerTitle(REWARD_DISTRIBUTION_TITLE)
                                                 .lstColumn(RewardDistribution.buildExportColumn())
                                                 .lstData(poolRegistrations)
                                                 .build());
  }


  @Async
  public CompletableFuture<ExportContent> exportPoolDeregistration(PoolReportHistory poolReport) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    List<PoolDeregistration> poolRegistrations = poolLifecycleService.deRegistration(
            poolReport.getPoolView(), defPageablePool)
        .stream()
        .map(PoolDeregistration::toDomain)
        .collect(Collectors.toList());

    return CompletableFuture.completedFuture(ExportContent.builder()
                                                 .clazz(PoolDeregistration.class)
                                                 .headerTitle(DEREGISTRATION_TITLE)
                                                 .lstColumn(PoolDeregistration.buildExportColumn())
                                                 .lstData(poolRegistrations)
                                                 .build());
  }
}
