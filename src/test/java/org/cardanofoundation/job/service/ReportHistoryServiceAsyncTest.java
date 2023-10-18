package org.cardanofoundation.job.service;

import java.sql.Timestamp;
import java.util.Collections;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ReportHistoryServiceAsyncTest {

  private static final String DELEGATION_HISTORY_TITLE = "Delegation History";
  private static final String WITHDRAWAL_HISTORY_TITLE = "Withdrawal History";
  private static final String WALLET_ACTIVITY_TITLE = "ADA Transfer";
  private static final String POOL_SIZE_TITLE = "Pool Size";
  private static final String REGISTRATIONS_TITLE = "Registrations";
  private static final String POOL_UPDATE_TITLE = "Pool Update";
  private static final String REWARD_DISTRIBUTION_TITLE = "Reward Distribution";
  private static final String DEREGISTRATION_TITLE = "Deregistration";

  @Mock
  StakeKeyLifeCycleService stakeKeyLifeCycleService;
  @Mock
  PoolLifecycleService poolLifecycleService;
  ReportHistoryServiceAsync reportHistoryServiceAsync;

  private Pageable defPageableStake;
  private Pageable defPageablePool;

  /**
   * Create a new instance of ReportHistoryServiceAsync before each test. Set the limitSize and
   * defPageableStake, defPageablePool to 1000 and Pageable.unpaged() respectively.
   */
  @BeforeEach
  void setUp() {
    reportHistoryServiceAsync = new ReportHistoryServiceAsync(stakeKeyLifeCycleService,
                                                              poolLifecycleService);
    defPageableStake = PageRequest.of(0, 1000, Sort.by("time").descending());
    defPageablePool = PageRequest.of(0, 1000, Sort.by("id").descending());
    ReflectionTestUtils.setField(reportHistoryServiceAsync, "limitSize", 1000);
    ReflectionTestUtils.setField(reportHistoryServiceAsync, "defPageableStake", defPageableStake);
    ReflectionTestUtils.setField(reportHistoryServiceAsync, "defPageablePool", defPageablePool);
  }

  @Test
  void exportStakeWalletActivityTest() {
    final String stakeKey = "stakeKey";
    final Timestamp fromDate = Timestamp.valueOf("2021-01-01 00:00:00");
    final Timestamp toDate = Timestamp.valueOf("2022-01-01 00:00:00");
    final Pageable pageable = Pageable.unpaged();
    final String subTitle = "sub-title";
    final StakeLifeCycleFilterRequest condition = StakeLifeCycleFilterRequest.builder()
        .fromDate(fromDate)
        .toDate(toDate)
        .build();

    when(stakeKeyLifeCycleService.getStakeWalletActivities(stakeKey, pageable, condition))
        .thenReturn(Collections.emptyList());

    var response = reportHistoryServiceAsync.exportStakeWalletActivitys(
        stakeKey, condition, pageable, subTitle).join();
    Assertions.assertEquals(WALLET_ACTIVITY_TITLE + subTitle, response.getHeaderTitle());
    Assertions.assertEquals(0, response.getLstData().size());
    Assertions.assertEquals(StakeWalletActivityResponse.class, response.getClazz());
  }

  @Test
  void exportStakeRegistrationsTest() {
    final String stakeKey = "stakeKey";
    final Timestamp fromDate = Timestamp.valueOf("2021-01-01 00:00:00");
    final Timestamp toDate = Timestamp.valueOf("2022-01-01 00:00:00");
    final StakeLifeCycleFilterRequest condition = StakeLifeCycleFilterRequest.builder()
        .fromDate(fromDate)
        .toDate(toDate)
        .build();

    when(stakeKeyLifeCycleService.getStakeRegistrations(stakeKey, defPageableStake, condition))
        .thenReturn(Collections.emptyList());

    var response = reportHistoryServiceAsync.exportStakeRegistrations(stakeKey, condition).join();
    Assertions.assertEquals(REGISTRATIONS_TITLE, response.getHeaderTitle());
    Assertions.assertEquals(0, response.getLstData().size());
    Assertions.assertEquals(StakeRegistrationLifeCycle.class, response.getClazz());
  }

  @Test
  void exportStakeDelegationsTest() {
    final String stakeKey = "stakeKey";
    final Timestamp fromDate = Timestamp.valueOf("2021-01-01 00:00:00");
    final Timestamp toDate = Timestamp.valueOf("2022-01-01 00:00:00");
    final StakeLifeCycleFilterRequest condition = StakeLifeCycleFilterRequest.builder()
        .fromDate(fromDate)
        .toDate(toDate)
        .build();

    when(stakeKeyLifeCycleService.getStakeDelegations(stakeKey, defPageableStake, condition))
        .thenReturn(Collections.emptyList());
    var response = reportHistoryServiceAsync.exportStakeDelegations(stakeKey, condition).join();
    Assertions.assertEquals(DELEGATION_HISTORY_TITLE, response.getHeaderTitle());
    Assertions.assertEquals(0, response.getLstData().size());
    Assertions.assertEquals(StakeDelegationFilterResponse.class, response.getClazz());
  }

  @Test
  void exportStakeRewardsTest() {
    final String stakeKey = "stakeKey";
    final Timestamp fromDate = Timestamp.valueOf("2021-01-01 00:00:00");
    final Timestamp toDate = Timestamp.valueOf("2022-01-01 00:00:00");
    final StakeLifeCycleFilterRequest condition = StakeLifeCycleFilterRequest.builder()
        .fromDate(fromDate)
        .toDate(toDate)
        .build();

    when(stakeKeyLifeCycleService.getStakeRewards(stakeKey, defPageablePool, condition))
        .thenReturn(Collections.emptyList());
    var response = reportHistoryServiceAsync.exportStakeRewards(stakeKey, condition).join();
    Assertions.assertEquals(REWARD_DISTRIBUTION_TITLE, response.getHeaderTitle());
    Assertions.assertEquals(0, response.getLstData().size());
    Assertions.assertEquals(StakeRewardResponse.class, response.getClazz());
  }

  @Test
  void exportStakeWithdrawalsTest() {
    final String stakeKey = "stakeKey";
    final Timestamp fromDate = Timestamp.valueOf("2021-01-01 00:00:00");
    final Timestamp toDate = Timestamp.valueOf("2022-01-01 00:00:00");
    final StakeLifeCycleFilterRequest condition = StakeLifeCycleFilterRequest.builder()
        .fromDate(fromDate)
        .toDate(toDate)
        .build();

    when(stakeKeyLifeCycleService.getStakeWithdrawals(stakeKey, defPageableStake, condition))
        .thenReturn(Collections.emptyList());
    var response = reportHistoryServiceAsync.exportStakeWithdrawals(stakeKey, condition).join();
    Assertions.assertEquals(WITHDRAWAL_HISTORY_TITLE, response.getHeaderTitle());
    Assertions.assertEquals(0, response.getLstData().size());
    Assertions.assertEquals(StakeWithdrawalFilterResponse.class, response.getClazz());
  }

  @Test
  void exportStakeDeregistrationsTest() {
    final String stakeKey = "stakeKey";
    final Timestamp fromDate = Timestamp.valueOf("2021-01-01 00:00:00");
    final Timestamp toDate = Timestamp.valueOf("2022-01-01 00:00:00");
    final StakeLifeCycleFilterRequest condition = StakeLifeCycleFilterRequest.builder()
        .fromDate(fromDate)
        .toDate(toDate)
        .build();

    when(stakeKeyLifeCycleService.getStakeDeRegistrations(stakeKey, defPageableStake, condition))
        .thenReturn(Collections.emptyList());
    var response = reportHistoryServiceAsync.exportStakeDeregistrations(stakeKey, condition).join();
    Assertions.assertEquals(DEREGISTRATION_TITLE, response.getHeaderTitle());
    Assertions.assertEquals(0, response.getLstData().size());
    Assertions.assertEquals(StakeRegistrationLifeCycle.class, response.getClazz());
  }

  @Test
  void exportEpochSizeTest() {
    final PoolReportHistory poolReportHistory = PoolReportHistory.builder()
        .build();
    when(poolLifecycleService.getPoolSizes(any(PoolReportHistory.class), any(Pageable.class)))
        .thenReturn(Collections.emptyList());
    var response = reportHistoryServiceAsync.exportEpochSize(poolReportHistory).join();
    Assertions.assertEquals(POOL_SIZE_TITLE, response.getHeaderTitle());
    Assertions.assertEquals(0, response.getLstData().size());
    Assertions.assertEquals(EpochSize.class, response.getClazz());
  }

  @Test
  void exportPoolRegistrationTest() {
    final PoolReportHistory poolReportHistory = PoolReportHistory.builder()
        .poolView("poolView")
        .build();
    when(poolLifecycleService.registrationList(poolReportHistory.getPoolView(), defPageablePool))
        .thenReturn(Collections.emptyList());
    var response = reportHistoryServiceAsync.exportPoolRegistration(poolReportHistory).join();
    Assertions.assertEquals(REGISTRATIONS_TITLE, response.getHeaderTitle());
    Assertions.assertEquals(0, response.getLstData().size());
    Assertions.assertEquals(PoolRegistration.class, response.getClazz());
  }

  @Test
  void exportPoolUpdateTest() {
    final PoolReportHistory poolReportHistory = PoolReportHistory.builder()
        .poolView("poolView")
        .build();
    when(poolLifecycleService.poolUpdateList(poolReportHistory.getPoolView(), defPageablePool))
        .thenReturn(Collections.emptyList());
    var response = reportHistoryServiceAsync.exportPoolUpdate(poolReportHistory).join();
    Assertions.assertEquals(POOL_UPDATE_TITLE, response.getHeaderTitle());
    Assertions.assertEquals(0, response.getLstData().size());
    Assertions.assertEquals(PoolUpdate.class, response.getClazz());
  }

  @Test
  void exportRewardsDistributionTest() {
    final PoolReportHistory poolReportHistory = PoolReportHistory.builder()
        .poolView("poolView")
        .build();
    when(poolLifecycleService.listReward(poolReportHistory, defPageablePool))
        .thenReturn(Collections.emptyList());
    var response = reportHistoryServiceAsync.exportRewardsDistribution(poolReportHistory).join();
    Assertions.assertEquals(REWARD_DISTRIBUTION_TITLE, response.getHeaderTitle());
    Assertions.assertEquals(0, response.getLstData().size());
    Assertions.assertEquals(RewardDistribution.class, response.getClazz());
  }

  @Test
  void exportPoolDeregistration() {
    final PoolReportHistory poolReportHistory = PoolReportHistory.builder()
        .poolView("poolView")
        .build();
    when(poolLifecycleService.deRegistration(poolReportHistory.getPoolView(), defPageablePool))
        .thenReturn(Collections.emptyList());
    var response = reportHistoryServiceAsync.exportPoolDeregistration(poolReportHistory).join();
    Assertions.assertEquals(DEREGISTRATION_TITLE, response.getHeaderTitle());
    Assertions.assertEquals(0, response.getLstData().size());
    Assertions.assertEquals(PoolDeregistration.class, response.getClazz());
  }
}