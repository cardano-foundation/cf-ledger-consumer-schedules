package org.cardanofoundation.job.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.explorer.common.entity.explorer.PoolReportHistory;
import org.cardanofoundation.explorer.common.entity.ledgersync.PoolUpdate;
import org.cardanofoundation.job.common.enumeration.PoolActionType;
import org.cardanofoundation.job.dto.PoolCertificateHistory;
import org.cardanofoundation.job.projection.LifeCycleRewardProjection;
import org.cardanofoundation.job.projection.PoolDeRegistrationProjection;
import org.cardanofoundation.job.projection.PoolInfoProjection;
import org.cardanofoundation.job.projection.PoolRegistrationProjection;
import org.cardanofoundation.job.projection.PoolUpdateDetailProjection;
import org.cardanofoundation.job.repository.ledgersync.PoolHashRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolRetireRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolUpdateRepository;
import org.cardanofoundation.job.repository.ledgersync.RewardRepository;
import org.cardanofoundation.job.service.FetchRewardDataService;
import org.cardanofoundation.job.service.PoolCertificateService;

@ExtendWith(MockitoExtension.class)
class PoolLifecycleServiceImplTest {

  @Mock private PoolHashRepository poolHashRepository;

  @Mock private PoolUpdateRepository poolUpdateRepository;

  @Mock private RewardRepository rewardRepository;

  @Mock private PoolRetireRepository poolRetireRepository;

  @Mock FetchRewardDataService fetchRewardDataService;

  @Mock PoolCertificateService poolCertificateService;

  @InjectMocks private PoolLifecycleServiceImpl poolLifecycleService;

  @Test
  void registrationList_shouldReturnRegistrationList() {
    Pageable pageable = PageRequest.of(0, 10);
    List<PoolCertificateHistory> poolCertificateHistoryList =
        List.of(
            PoolCertificateHistory.builder()
                .poolUpdateId(69L)
                .txHash("d867f77bb62fe58df4b13285f6b8d37a8aae41eea662b248b80321ec5ce60asda")
                .build());
    PoolRegistrationProjection registrationProjection =
        Mockito.mock(PoolRegistrationProjection.class);
    when(registrationProjection.getPoolUpdateId()).thenReturn(69L);
    when(registrationProjection.getTxHash())
        .thenReturn("d867f77bb62fe58df4b13285f6b8d37a8aae41eea662b248b80321ec5ce60asda");
    when(registrationProjection.getFee()).thenReturn(BigInteger.TEN);
    when(registrationProjection.getDeposit()).thenReturn(BigInteger.valueOf(500));
    when(poolCertificateService.getPoolCertificateByAction(
            "pool1h0anq89dytn6vtm0afhreyawcnn0w99w7e4s4q5w0yh3ymzh94s",
            PoolActionType.POOL_REGISTRATION))
        .thenReturn(poolCertificateHistoryList);

    when(poolHashRepository.getPoolRegistrationByPool(anySet(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(registrationProjection)));

    var response =
        poolLifecycleService.registrationList(
            "pool1h0anq89dytn6vtm0afhreyawcnn0w99w7e4s4q5w0yh3ymzh94s", pageable);

    Assertions.assertEquals(
        "d867f77bb62fe58df4b13285f6b8d37a8aae41eea662b248b80321ec5ce60asda",
        response.get(0).getTxHash());
  }

  @Test
  void poolUpdateList_shouldReturnResponse() {
    Pageable pageable = PageRequest.of(0, 10);
    PoolUpdateDetailProjection projection = Mockito.mock(PoolUpdateDetailProjection.class);
    when(projection.getTxHash())
        .thenReturn("d867f77bb62fe58df4b13285f6b8d37a8aae41eea662b248b80321ec5ce60asda");
    when(projection.getHashId()).thenReturn(1L);
    when(projection.getPledge()).thenReturn(BigInteger.TEN);
    when(projection.getFee()).thenReturn(BigInteger.TEN);
    when(projection.getPoolName()).thenReturn("Test");
    when(projection.getRewardAccount())
        .thenReturn("stake1u80n7nvm3qlss9ls0krp5xh7sqxlazp8kz6n3fp5sgnul5cnxyg4p");
    when(projection.getCost()).thenReturn(BigInteger.TEN);
    when(projection.getMargin()).thenReturn(1.0);
    when(projection.getVrfKey())
        .thenReturn("d867f77bb62fe58df4b13285f6b8d37a8aae41eea662b248b80321ec5ce60asda");
    when(projection.getPoolUpdateId()).thenReturn(69L);
    List<PoolCertificateHistory> poolCertificateHistoryList =
        List.of(
            PoolCertificateHistory.builder()
                .poolUpdateId(69L)
                .txHash("d867f77bb62fe58df4b13285f6b8d37a8aae41eea662b248b80321ec5ce60asda")
                .build());

    when(poolCertificateService.getPoolCertificateByAction(
            "pool1h0anq89dytn6vtm0afhreyawcnn0w99w7e4s4q5w0yh3ymzh94s", PoolActionType.POOL_UPDATE))
        .thenReturn(poolCertificateHistoryList);

    when(poolUpdateRepository.findPoolUpdateByPool(anySet(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(projection)));

    when(poolUpdateRepository.findOwnerAccountByPoolUpdate(69L))
        .thenReturn(List.of("stake1u80n7nvm3qlss9ls0krp5xh7sqxlazp8kz6n3fp5sgnul5cnxyg4p"));
    PoolUpdate poolUpdate = Mockito.mock(PoolUpdate.class);
    when(poolUpdate.getPledge()).thenReturn(BigInteger.TWO);
    when(poolUpdate.getMargin()).thenReturn(1.5);
    when(poolUpdateRepository.findTopByIdLessThanAndPoolHashIdOrderByIdDesc(69L, 1L))
        .thenReturn(poolUpdate);

    var response =
        poolLifecycleService.poolUpdateList(
            "pool1h0anq89dytn6vtm0afhreyawcnn0w99w7e4s4q5w0yh3ymzh94s", pageable);
    Assertions.assertEquals(
        "d867f77bb62fe58df4b13285f6b8d37a8aae41eea662b248b80321ec5ce60asda",
        response.get(0).getTxHash());
  }

  @Test
  void listReward_shouldReturnResponse() {
    Timestamp time = Timestamp.valueOf("2023-01-01 00:00:00");
    Pageable pageable = PageRequest.of(0, 10);
    LifeCycleRewardProjection projection = Mockito.mock(LifeCycleRewardProjection.class);
    PoolReportHistory poolReportHistory =
        PoolReportHistory.builder()
            .beginEpoch(1)
            .endEpoch(100)
            .poolView("pool1h0anq89dytn6vtm0afhreyawcnn0w99w7e4s4q5w0yh3ymzh94s")
            .build();
    when(projection.getAddress())
        .thenReturn("stake1u80n7nvm3qlss9ls0krp5xh7sqxlazp8kz6n3fp5sgnul5cnxyg4p");
    when(projection.getAmount()).thenReturn(BigInteger.TWO);
    when(projection.getEpochNo()).thenReturn(69);
    when(projection.getTime()).thenReturn(time);
    when(rewardRepository.getRewardInfoByPool(
            poolReportHistory.getPoolView(),
            poolReportHistory.getBeginEpoch(),
            poolReportHistory.getEndEpoch(),
            pageable))
        .thenReturn(new PageImpl<>(List.of(projection)));

    when(fetchRewardDataService.fetchReward(anyString())).thenReturn(Boolean.TRUE);
    var response = poolLifecycleService.listReward(poolReportHistory, pageable);

    Assertions.assertEquals(69, response.get(0).getEpochNo());
    Assertions.assertEquals(time, response.get(0).getTime());
    Assertions.assertEquals(BigInteger.TWO, response.get(0).getAmount());
  }

  @Test
  void deRegistration_returnDeRegistrationList() {
    Pageable pageable = PageRequest.of(0, 10);
    PoolDeRegistrationProjection retireProjection =
        Mockito.mock(PoolDeRegistrationProjection.class);
    when(retireProjection.getRetiringEpoch()).thenReturn(69);
    when(retireProjection.getTxHash())
        .thenReturn("d867f77bb62fe58df4b13285f6b8d37a8aae41eea662b248b80321ec5ce60asda");
    when(retireProjection.getFee()).thenReturn(BigInteger.TEN);

    List<PoolCertificateHistory> poolCertificateHistoryList =
        List.of(
            PoolCertificateHistory.builder()
                .poolUpdateId(69L)
                .txHash("d867f77bb62fe58df4b13285f6b8d37a8aae41eea662b248b80321ec5ce60asda")
                .build());

    when(poolCertificateService.getPoolCertificateByAction(
            "pool1h0anq89dytn6vtm0afhreyawcnn0w99w7e4s4q5w0yh3ymzh94s",
            PoolActionType.POOL_DEREGISTRATION))
        .thenReturn(poolCertificateHistoryList);

    when(poolRetireRepository.getPoolDeRegistration(anySet(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(retireProjection)));

    PoolInfoProjection projection = Mockito.mock(PoolInfoProjection.class);
    when(projection.getPoolView())
        .thenReturn("pool1h0anq89dytn6vtm0afhreyawcnn0w99w7e4s4q5w0yh3ymzh94s");
    when(projection.getPoolName()).thenReturn("Test");
    when(projection.getHashRaw())
        .thenReturn("d867f77bb62fe58df4b13285f6b8d37a8aae41eea662b248b80321ec5ce60asda");
    when(poolHashRepository.getPoolInfo("pool1h0anq89dytn6vtm0afhreyawcnn0w99w7e4s4q5w0yh3ymzh94s"))
        .thenReturn(projection);
    var response =
        poolLifecycleService.deRegistration(
            "pool1h0anq89dytn6vtm0afhreyawcnn0w99w7e4s4q5w0yh3ymzh94s", pageable);

    Assertions.assertEquals(69, response.get(0).getRetiringEpoch());
    Assertions.assertEquals(BigInteger.TEN, response.get(0).getFee());
    Assertions.assertEquals("Test", response.get(0).getPoolName());
    Assertions.assertEquals(
        "d867f77bb62fe58df4b13285f6b8d37a8aae41eea662b248b80321ec5ce60asda",
        response.get(0).getTxHash());
  }
}
