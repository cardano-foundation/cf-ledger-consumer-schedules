package org.cardanofoundation.job.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.conversions.ClasspathConversionsFactory;
import org.cardanofoundation.conversions.domain.NetworkType;
import org.cardanofoundation.explorer.common.entity.ledgersync.EpochParam;
import org.cardanofoundation.explorer.common.entity.ledgersync.StakeAddress;
import org.cardanofoundation.explorer.common.entity.ledgersync.Tx;
import org.cardanofoundation.job.dto.report.stake.StakeLifeCycleFilterRequest;
import org.cardanofoundation.job.projection.StakeDelegationProjection;
import org.cardanofoundation.job.projection.StakeHistoryProjection;
import org.cardanofoundation.job.projection.StakeRewardProjection;
import org.cardanofoundation.job.projection.StakeTxProjection;
import org.cardanofoundation.job.projection.StakeWithdrawalProjection;
import org.cardanofoundation.job.repository.ledgersync.AddressTxAmountRepository;
import org.cardanofoundation.job.repository.ledgersync.DelegationRepository;
import org.cardanofoundation.job.repository.ledgersync.EpochParamRepository;
import org.cardanofoundation.job.repository.ledgersync.RewardRepository;
import org.cardanofoundation.job.repository.ledgersync.StakeAddressRepository;
import org.cardanofoundation.job.repository.ledgersync.StakeDeRegistrationRepository;
import org.cardanofoundation.job.repository.ledgersync.StakeRegistrationRepository;
import org.cardanofoundation.job.repository.ledgersync.TxRepository;
import org.cardanofoundation.job.repository.ledgersync.WithdrawalRepository;
import org.cardanofoundation.job.service.FetchRewardDataService;
import org.cardanofoundation.job.util.DateUtils;

@ExtendWith(MockitoExtension.class)
class StakeKeyLifeCycleServiceImplTest {

  StakeAddress stakeAddress =
      StakeAddress.builder()
          .view("stake1u98ujxfgzdm8yh6qsaar54nmmr50484t4ytphxjex3zxh7g4tuwna")
          .availableReward(BigInteger.valueOf(0))
          .build();
  @Mock private DelegationRepository delegationRepository;
  @Mock private StakeRegistrationRepository stakeRegistrationRepository;
  @Mock private StakeAddressRepository stakeAddressRepository;
  @Mock private RewardRepository rewardRepository;
  @Mock private StakeDeRegistrationRepository stakeDeRegistrationRepository;
  @Mock private WithdrawalRepository withdrawalRepository;
  @Mock private AddressTxAmountRepository addressTxAmountRepository;
  @Mock private TxRepository txRepository;
  @Mock private EpochParamRepository epochParamRepository;
  @InjectMocks private StakeKeyLifeCycleServiceImpl stakeKeyLifeCycleService;

  @Mock private FetchRewardDataService fetchRewardDataService;

  CardanoConverters cardanoConverters =
      ClasspathConversionsFactory.createConverters(NetworkType.MAINNET);

  @BeforeEach
  public void setUp() {
    ReflectionTestUtils.setField(stakeKeyLifeCycleService, "cardanoConverters", cardanoConverters);
  }

  @Test
  void
      getStakeWalletActivities_whenStakeAddressHaveRegistrationWithCondition_showReturnRegistrations() {
    Pageable pageable = PageRequest.of(0, 1);
    Timestamp fromDate = Timestamp.valueOf("2023-01-01 00:00:00");
    Timestamp toDate =
        Timestamp.from(
            LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toInstant(ZoneOffset.UTC));
    StakeLifeCycleFilterRequest condition =
        StakeLifeCycleFilterRequest.builder().fromDate(fromDate).toDate(toDate).build();

    StakeHistoryProjection projection = Mockito.mock(StakeHistoryProjection.class);
    when(projection.getTxHash())
        .thenReturn("f8680884f04ef2b10fdc778e2aa981b909f7268570db231a1d0baac377620ea2");
    when(projection.getFee()).thenReturn(BigInteger.valueOf(173333));
    when(projection.getEpochNo()).thenReturn(333);
    when(projection.getTime()).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
    Page<StakeHistoryProjection> page = new PageImpl<>(List.of(projection), pageable, 1);
    when(stakeAddressRepository.findByView(anyString())).thenReturn(stakeAddress);
    when(stakeRegistrationRepository.getStakeRegistrationsByAddress(
            stakeAddress, null, fromDate, toDate, pageable))
        .thenReturn(page);

    when(epochParamRepository.findByEpochNoIn(List.of(333)))
        .thenReturn(
            List.of(
                EpochParam.builder().epochNo(333).keyDeposit(BigInteger.valueOf(2000000)).build()));

    var response =
        stakeKeyLifeCycleService.getStakeRegistrations(
            "stake1u98ujxfgzdm8yh6qsaar54nmmr50484t4ytphxjex3zxh7g4tuwna", pageable, condition);

    Assertions.assertEquals(1, response.size());
    Assertions.assertEquals(BigInteger.valueOf(173333), response.get(0).getFee());
    Assertions.assertEquals(2000000L, response.get(0).getDeposit());
  }

  @Test
  void getStakeWalletActivities_whenGetStakeWalletActivities_showReturnWalletActivities() {
    Pageable pageable = PageRequest.of(0, 6);
    StakeTxProjection projection = Mockito.mock(StakeTxProjection.class);
    when(projection.getTxId()).thenReturn(100L);
    when(projection.getAmount()).thenReturn(BigInteger.valueOf(-500174301));
    when(projection.getTime())
        .thenReturn(DateUtils.timestampToEpochSecond(Timestamp.valueOf(LocalDateTime.now())));
    StakeTxProjection projection1 = Mockito.mock(StakeTxProjection.class);
    when(projection1.getTxId()).thenReturn(101L);
    when(projection1.getAmount()).thenReturn(BigInteger.valueOf(72960943));
    when(projection1.getTime())
        .thenReturn(DateUtils.timestampToEpochSecond(Timestamp.valueOf(LocalDateTime.now())));
    StakeTxProjection projection2 = Mockito.mock(StakeTxProjection.class);
    when(projection2.getTxId()).thenReturn(102L);
    when(projection2.getAmount()).thenReturn(BigInteger.valueOf(-2174301));
    when(projection2.getTime())
        .thenReturn(DateUtils.timestampToEpochSecond(Timestamp.valueOf(LocalDateTime.now())));
    StakeTxProjection projection3 = Mockito.mock(StakeTxProjection.class);
    when(projection3.getTxId()).thenReturn(103L);
    when(projection3.getAmount()).thenReturn(BigInteger.valueOf(-181385));
    when(projection3.getTime())
        .thenReturn(DateUtils.timestampToEpochSecond(Timestamp.valueOf(LocalDateTime.now())));
    StakeTxProjection projection4 = Mockito.mock(StakeTxProjection.class);
    when(projection4.getTxId()).thenReturn(104L);
    when(projection4.getAmount()).thenReturn(BigInteger.valueOf(-172761));
    when(projection4.getTime())
        .thenReturn(DateUtils.timestampToEpochSecond(Timestamp.valueOf(LocalDateTime.now())));
    StakeTxProjection projection5 = Mockito.mock(StakeTxProjection.class);
    when(projection5.getTxId()).thenReturn(105L);
    when(projection5.getAmount()).thenReturn(BigInteger.valueOf(-2174301));
    when(projection5.getTime())
        .thenReturn(DateUtils.timestampToEpochSecond(Timestamp.valueOf(LocalDateTime.now())));
    Page<StakeTxProjection> page =
        new PageImpl<>(
            List.of(projection, projection1, projection2, projection3, projection4, projection5),
            pageable,
            6);
    List<Tx> txList = new ArrayList<>();
    txList.add(
        Tx.builder()
            .id(100L)
            .hash("11ae03377b31c749d2d549674100986ec4ee68ac72e211404647f5ae0ce8686b")
            .fee(BigInteger.valueOf(174301))
            .deposit(0L)
            .validContract(true)
            .build());
    txList.add(
        Tx.builder()
            .id(101L)
            .hash("3a4de98d4652281ff2c747bbe0582c985d590ca57bc783fa3e5e0c23b126d6ca")
            .fee(BigInteger.valueOf(175093))
            .deposit(0L)
            .validContract(true)
            .build());
    txList.add(
        Tx.builder()
            .id(102L)
            .hash("17c5b738f4de8a67882791d261f7fcbd6671e4eae29936171ac48307c18d191e")
            .fee(BigInteger.valueOf(174301))
            .validContract(true)
            .deposit(2000000L)
            .build());
    txList.add(
        Tx.builder()
            .id(103L)
            .hash("5b995ad32ba2c0bb86e224441845d8adc71a03be932360b93e1a04bd459b02da")
            .fee(BigInteger.valueOf(-181385))
            .deposit(0L)
            .validContract(false)
            .build());
    txList.add(
        Tx.builder()
            .id(104L)
            .hash("e985489b135b68add6f0f13a3e3b7f513f9e56e4710faee8b0c5065afb4419d1")
            .fee(BigInteger.valueOf(172761))
            .deposit(0L)
            .validContract(false)
            .build());
    txList.add(
        Tx.builder()
            .id(105L)
            .hash("817c26fc41a840f640c83ddda096a51406649402fc7dde0739131b209e9432b6")
            .fee(BigInteger.valueOf(24027))
            .deposit(-2000000L)
            .validContract(false)
            .build());

    Timestamp fromDate = Timestamp.valueOf("2023-01-01 00:00:00");
    Timestamp toDate =
        Timestamp.from(
            LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toInstant(ZoneOffset.UTC));
    StakeLifeCycleFilterRequest condition =
        StakeLifeCycleFilterRequest.builder().fromDate(fromDate).toDate(toDate).build();

    var slotFrom = cardanoConverters.time().toSlot(condition.getFromDate().toLocalDateTime());
    var slotTo = cardanoConverters.time().toSlot(condition.getToDate().toLocalDateTime());

    when(addressTxAmountRepository.findTxAndAmountByStakeBetweenSlots(
            stakeAddress.getView(), slotFrom, slotTo, pageable))
        .thenReturn(page);

    when(txRepository.findByIdIn(any())).thenReturn(txList);

    var response =
        stakeKeyLifeCycleService.getStakeWalletActivities(
            stakeAddress.getView(), pageable, condition);

    Assertions.assertEquals(6, response.size());
    Assertions.assertEquals(
        response.get(0).getTxHash(),
        "11ae03377b31c749d2d549674100986ec4ee68ac72e211404647f5ae0ce8686b");
    Assertions.assertEquals(
        response.get(1).getTxHash(),
        "3a4de98d4652281ff2c747bbe0582c985d590ca57bc783fa3e5e0c23b126d6ca");
    Assertions.assertEquals(
        response.get(2).getTxHash(),
        "17c5b738f4de8a67882791d261f7fcbd6671e4eae29936171ac48307c18d191e");
    Assertions.assertEquals(
        response.get(3).getTxHash(),
        "5b995ad32ba2c0bb86e224441845d8adc71a03be932360b93e1a04bd459b02da");
    Assertions.assertEquals(
        response.get(4).getTxHash(),
        "e985489b135b68add6f0f13a3e3b7f513f9e56e4710faee8b0c5065afb4419d1");
    Assertions.assertEquals(
        response.get(5).getTxHash(),
        "817c26fc41a840f640c83ddda096a51406649402fc7dde0739131b209e9432b6");
  }

  @Test
  void
      getStakeRegistrations_whenStakeAddressHaveRegistrationWithCondition_showReturnRegistrations() {
    Pageable pageable = PageRequest.of(0, 1);
    Timestamp fromDate = Timestamp.valueOf("2023-01-01 00:00:00");
    Timestamp toDate =
        Timestamp.from(
            LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toInstant(ZoneOffset.UTC));
    StakeLifeCycleFilterRequest condition =
        StakeLifeCycleFilterRequest.builder().fromDate(fromDate).toDate(toDate).build();

    StakeHistoryProjection projection = Mockito.mock(StakeHistoryProjection.class);
    when(projection.getTxHash())
        .thenReturn("f8680884f04ef2b10fdc778e2aa981b909f7268570db231a1d0baac377620ea2");
    when(projection.getFee()).thenReturn(BigInteger.valueOf(173333));
    when(projection.getEpochNo()).thenReturn(333);
    when(projection.getTime()).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
    Page<StakeHistoryProjection> page = new PageImpl<>(List.of(projection), pageable, 1);
    when(stakeAddressRepository.findByView(anyString())).thenReturn(stakeAddress);
    when(stakeRegistrationRepository.getStakeRegistrationsByAddress(
            stakeAddress, null, fromDate, toDate, pageable))
        .thenReturn(page);

    when(epochParamRepository.findByEpochNoIn(List.of(333)))
        .thenReturn(
            List.of(
                EpochParam.builder().epochNo(333).keyDeposit(BigInteger.valueOf(2000000)).build()));

    var response =
        stakeKeyLifeCycleService.getStakeRegistrations(
            "stake1u98ujxfgzdm8yh6qsaar54nmmr50484t4ytphxjex3zxh7g4tuwna", pageable, condition);

    Assertions.assertEquals(1, response.size());
    Assertions.assertEquals(BigInteger.valueOf(173333), response.get(0).getFee());
    Assertions.assertEquals(2000000L, response.get(0).getDeposit());
  }

  @Test
  void getStakeDelegations_whenStakeAddressHaveDelegationWithCondition_showReturnDelegations() {
    Pageable pageable = PageRequest.of(0, 1);
    Timestamp fromDate = Timestamp.valueOf("2023-01-01 00:00:00");
    Timestamp toDate =
        Timestamp.from(
            LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toInstant(ZoneOffset.UTC));
    StakeLifeCycleFilterRequest condition =
        StakeLifeCycleFilterRequest.builder().fromDate(fromDate).toDate(toDate).build();

    StakeDelegationProjection projection = Mockito.mock(StakeDelegationProjection.class);
    when(projection.getTxHash())
        .thenReturn("bd80f5d56419eed99b45b45c58468213be28584ce64fcd2b6bd1300af8b6e488");
    when(projection.getOutSum()).thenReturn(BigInteger.valueOf(102569063));
    when(projection.getFee()).thenReturn(BigInteger.valueOf(12344));
    when(projection.getTime()).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
    Page<StakeDelegationProjection> page = new PageImpl<>(List.of(projection), pageable, 1);
    when(stakeAddressRepository.findByView(anyString())).thenReturn(stakeAddress);
    when(delegationRepository.findDelegationByAddress(any(), any(), any(), any(), any()))
        .thenReturn(page);
    var response =
        stakeKeyLifeCycleService.getStakeDelegations(
            "stake1u98ujxfgzdm8yh6qsaar54nmmr50484t4ytphxjex3zxh7g4tuwna", pageable, condition);
    Assertions.assertEquals(1, response.size());
    Assertions.assertEquals(BigInteger.valueOf(102569063), response.get(0).getOutSum());
    Assertions.assertEquals(BigInteger.valueOf(12344), response.get(0).getFee());
  }

  @Test
  void getStakeRewards_whenStakeAddressHaveRewards_showReturnRewards() {
    Pageable pageable = PageRequest.of(0, 1);
    StakeRewardProjection projection = Mockito.mock(StakeRewardProjection.class);
    when(projection.getEpoch()).thenReturn(333);
    when(projection.getAmount()).thenReturn(BigInteger.valueOf(382916));
    when(projection.getTime()).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
    Timestamp fromDate = Timestamp.valueOf("2023-01-01 00:00:00");
    Timestamp toDate =
        Timestamp.from(
            LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toInstant(ZoneOffset.UTC));
    StakeLifeCycleFilterRequest condition =
        StakeLifeCycleFilterRequest.builder().fromDate(fromDate).toDate(toDate).build();
    Page<StakeRewardProjection> page = new PageImpl<>(List.of(projection), pageable, 1);
    when(stakeAddressRepository.findByView(anyString())).thenReturn(stakeAddress);
    when(rewardRepository.findRewardByStake(stakeAddress, fromDate, toDate, pageable))
        .thenReturn(page);

    when(fetchRewardDataService.checkRewardAvailable(any())).thenReturn(true);

    var response =
        stakeKeyLifeCycleService.getStakeRewards(
            "stake1u98ujxfgzdm8yh6qsaar54nmmr50484t4ytphxjex3zxh7g4tuwna", pageable, condition);

    Assertions.assertEquals(333, response.get(0).getEpoch());
    Assertions.assertEquals(BigInteger.valueOf(382916), response.get(0).getAmount());
  }

  @Test
  void getStakeWithdrawals_whenStakeAddressHaveWithdrawalWithCondition_showReturnWithdrawal() {
    Pageable pageable = PageRequest.of(0, 1);
    Timestamp fromDate = Timestamp.valueOf("2023-01-01 00:00:00");
    Timestamp toDate =
        Timestamp.from(
            LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toInstant(ZoneOffset.UTC));
    StakeLifeCycleFilterRequest condition =
        StakeLifeCycleFilterRequest.builder().fromDate(fromDate).toDate(toDate).build();
    StakeWithdrawalProjection projection = Mockito.mock(StakeWithdrawalProjection.class);
    when(projection.getTxHash())
        .thenReturn("91d4995345d7aa62f74167d22f596dbd10f486785be3605b0d3bc0ec1bd9c381");
    when(projection.getAmount()).thenReturn(BigInteger.valueOf(4846486));
    when(projection.getTime()).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
    when(projection.getFee()).thenReturn(BigInteger.valueOf(12344));
    Page<StakeWithdrawalProjection> page = new PageImpl<>(List.of(projection), pageable, 1);
    when(stakeAddressRepository.findByView(anyString())).thenReturn(stakeAddress);
    when(withdrawalRepository.getWithdrawalByAddress(any(), any(), any(), any(), any()))
        .thenReturn(page);
    var response =
        stakeKeyLifeCycleService.getStakeWithdrawals(
            "stake1u98ujxfgzdm8yh6qsaar54nmmr50484t4ytphxjex3zxh7g4tuwna", pageable, condition);

    Assertions.assertEquals(1, response.size());
    Assertions.assertEquals(
        "91d4995345d7aa62f74167d22f596dbd10f486785be3605b0d3bc0ec1bd9c381",
        response.get(0).getTxHash());
  }

  @Test
  void
      getStakeDeRegistrations_whenStakeAddressHaveDeRegistrationWithCondition_showReturnDeRegistrations() {
    Pageable pageable = PageRequest.of(0, 1);
    Timestamp fromDate = Timestamp.valueOf("2023-01-01 00:00:00");
    Timestamp toDate =
        Timestamp.from(
            LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toInstant(ZoneOffset.UTC));
    StakeLifeCycleFilterRequest condition =
        StakeLifeCycleFilterRequest.builder().fromDate(fromDate).toDate(toDate).build();
    StakeHistoryProjection projection = Mockito.mock(StakeHistoryProjection.class);
    when(projection.getTxHash())
        .thenReturn("f8680884f04ef2b10fdc778e2aa981b909f7268570db231a1d0baac377620ea2");
    when(projection.getFee()).thenReturn(BigInteger.valueOf(173333));
    when(projection.getEpochNo()).thenReturn(333);
    when(projection.getTime()).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
    Page<StakeHistoryProjection> page = new PageImpl<>(List.of(projection), pageable, 1);
    when(stakeAddressRepository.findByView(anyString())).thenReturn(stakeAddress);
    when(stakeDeRegistrationRepository.getStakeDeRegistrationsByAddress(
            stakeAddress, null, fromDate, toDate, pageable))
        .thenReturn(page);

    when(epochParamRepository.findByEpochNoIn(List.of(333)))
        .thenReturn(
            List.of(
                EpochParam.builder().epochNo(333).keyDeposit(BigInteger.valueOf(2000000)).build()));

    var response =
        stakeKeyLifeCycleService.getStakeDeRegistrations(
            "stake1u98ujxfgzdm8yh6qsaar54nmmr50484t4ytphxjex3zxh7g4tuwna", pageable, condition);

    Assertions.assertEquals(1, response.size());
    Assertions.assertEquals(
        "f8680884f04ef2b10fdc778e2aa981b909f7268570db231a1d0baac377620ea2",
        response.get(0).getTxHash());
    Assertions.assertEquals(BigInteger.valueOf(173333), response.get(0).getFee());
    Assertions.assertEquals(2000000L, response.get(0).getDeposit());
  }
}
