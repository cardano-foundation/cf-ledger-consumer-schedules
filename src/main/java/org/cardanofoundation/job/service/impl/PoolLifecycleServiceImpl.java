package org.cardanofoundation.job.service.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.cardanofoundation.explorer.consumercommon.explorer.entity.PoolReportHistory;
import org.cardanofoundation.explorer.consumercommon.entity.PoolUpdate;
import org.cardanofoundation.job.common.enumeration.PoolActionType;
import org.cardanofoundation.job.dto.PoolCertificateHistory;
import org.cardanofoundation.job.dto.report.pool.DeRegistrationResponse;
import org.cardanofoundation.job.dto.report.pool.EpochSize;
import org.cardanofoundation.job.dto.report.pool.PoolUpdateDetailResponse;
import org.cardanofoundation.job.dto.report.pool.RewardResponse;
import org.cardanofoundation.job.dto.report.pool.TabularRegisResponse;
import org.cardanofoundation.job.projection.EpochRewardProjection;
import org.cardanofoundation.job.repository.ledgersync.EpochStakeRepository;
import org.cardanofoundation.job.projection.LifeCycleRewardProjection;
import org.cardanofoundation.job.projection.PoolDeRegistrationProjection;
import org.cardanofoundation.job.projection.PoolHistoryKoiOsProjection;
import org.cardanofoundation.job.projection.PoolInfoProjection;
import org.cardanofoundation.job.projection.PoolRegistrationProjection;
import org.cardanofoundation.job.projection.PoolUpdateDetailProjection;
import org.cardanofoundation.job.repository.ledgersync.PoolHashRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolHistoryRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolRetireRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolUpdateRepository;
import org.cardanofoundation.job.repository.ledgersync.RewardRepository;
import org.cardanofoundation.job.service.FetchRewardDataService;
import org.cardanofoundation.job.service.PoolCertificateService;
import org.cardanofoundation.job.service.PoolLifecycleService;

@Service
@Log4j2
@RequiredArgsConstructor
public class PoolLifecycleServiceImpl implements PoolLifecycleService {

  private final PoolHashRepository poolHashRepository;
  private final PoolUpdateRepository poolUpdateRepository;
  private final RewardRepository rewardRepository;
  private final PoolRetireRepository poolRetireRepository;
  private final FetchRewardDataService fetchRewardDataService;
  private final PoolHistoryRepository poolHistoryRepository;
  private final EpochStakeRepository epochStakeRepository;
  private final PoolCertificateService poolCertificateService;

  @Override
  public List<TabularRegisResponse> registrationList(String poolView, Pageable pageable) {
    List<TabularRegisResponse> tabularRegisList = new ArrayList<>();
    List<PoolCertificateHistory> poolRegistration = poolCertificateService.getPoolCertificateByAction(
        poolView, PoolActionType.POOL_REGISTRATION);

    Page<PoolRegistrationProjection> projection = poolHashRepository
        .getPoolRegistrationByPool(poolRegistration.isEmpty() ? Set.of(-1L)
                                                              : poolRegistration.stream()
                                       .map(PoolCertificateHistory::getPoolUpdateId)
                                       .collect(Collectors.toSet()), pageable);
    if (Objects.nonNull(projection)) {
      projection.stream()
          .forEach(tabularRegis -> tabularRegisList.add(new TabularRegisResponse(tabularRegis)));
    }
    return tabularRegisList;
  }

  @Override
  public List<PoolUpdateDetailResponse> poolUpdateList(String poolView, Pageable pageable) {
    List<PoolUpdateDetailResponse> poolUpdateList = new ArrayList<>();
    List<PoolCertificateHistory> poolUpdateCert = poolCertificateService.getPoolCertificateByAction(
        poolView, PoolActionType.POOL_UPDATE);
    Page<PoolUpdateDetailProjection> projection = poolUpdateRepository.findPoolUpdateByPool(
        poolUpdateCert.isEmpty() ? Set.of(-1L)
                                 : poolUpdateCert.stream().map(PoolCertificateHistory::getPoolUpdateId).collect(
                                     Collectors.toSet()), pageable);
    if (Objects.nonNull(projection)) {
      projection.stream()
          .forEach(
              poolUpdate -> {
                PoolUpdateDetailResponse poolUpdateRes = new PoolUpdateDetailResponse(poolUpdate);
                poolUpdateRes.setStakeKeys(
                    poolUpdateRepository.findOwnerAccountByPoolUpdate(
                        poolUpdate.getPoolUpdateId()));
                PoolUpdate poolUpdatePrevious =
                    poolUpdateRepository.findTopByIdLessThanAndPoolHashIdOrderByIdDesc(
                        poolUpdate.getPoolUpdateId(), poolUpdate.getHashId());
                if (Objects.nonNull(poolUpdatePrevious)) {
                  poolUpdateRes.setPreviousPledge(poolUpdatePrevious.getPledge());
                  poolUpdateRes.setPreviousMargin(poolUpdatePrevious.getMargin());
                }
                poolUpdateList.add(poolUpdateRes);
              });
    }
    return poolUpdateList;
  }

  @Override
  public List<RewardResponse> listReward(PoolReportHistory poolReportHistory, Pageable pageable) {
    List<RewardResponse> rewardRes = new ArrayList<>();

    if (!fetchRewardDataService.fetchReward(poolReportHistory.getPoolView())) {
      throw new RuntimeException("Fetch reward failed");
    }

    Page<LifeCycleRewardProjection> projections =
        rewardRepository.getRewardInfoByPool(poolReportHistory.getPoolView(),
                                             poolReportHistory.getBeginEpoch(),
                                             poolReportHistory.getEndEpoch(),
                                             pageable);
    if (Objects.nonNull(projections)) {
      projections.stream()
          .forEach(
              projection -> {
                RewardResponse reward = new RewardResponse(projection);
                rewardRes.add(reward);
              });
    }
    return rewardRes;
  }

  @Override
  public List<DeRegistrationResponse> deRegistration(String poolView, Pageable pageable) {
    PoolInfoProjection poolInfo = poolHashRepository.getPoolInfo(poolView);

    List<PoolCertificateHistory> poolRetire = poolCertificateService.getPoolCertificateByAction(
        poolView, PoolActionType.POOL_DEREGISTRATION);
    Page<PoolDeRegistrationProjection> projections = poolRetireRepository.getPoolDeRegistration(
        poolRetire.isEmpty() ? Set.of(-1L)
                             : poolRetire.stream().map(PoolCertificateHistory::getPoolRetireId).collect(
                                 Collectors.toSet()), pageable);

    List<DeRegistrationResponse> deRegistrations = new ArrayList<>();
    if (Objects.nonNull(projections)) {
      Set<Integer> epochNos = new HashSet<>();
      projections.stream()
          .forEach(
              projection -> {
                DeRegistrationResponse deRegistrationRes = new DeRegistrationResponse(projection);
                deRegistrations.add(deRegistrationRes);
                epochNos.add(projection.getRetiringEpoch());
              });

      if (!fetchRewardDataService.fetchReward(poolView)) {
        throw new RuntimeException("Fetch reward failed");
      }

      List<EpochRewardProjection> epochRewardProjections =
          rewardRepository.getRewardRefundByEpoch(poolView, epochNos);
      Map<Integer, BigInteger> refundAmountMap = new HashMap<>();
      epochRewardProjections.forEach(
          refund -> refundAmountMap.put(refund.getEpochNo(), refund.getAmount()));
      deRegistrations.forEach(
          deRegistration -> {
            deRegistration.setPoolHold(refundAmountMap.get(deRegistration.getRetiringEpoch()));
            BigInteger totalFee = BigInteger.ZERO;
            if (Objects.nonNull(deRegistration.getPoolHold())) {
              totalFee = totalFee.add(deRegistration.getPoolHold());
            }
            if (Objects.nonNull(deRegistration.getFee())) {
              totalFee = totalFee.add(deRegistration.getFee());
            }
            deRegistration.setTotalFee(totalFee);
            deRegistration.setPoolId(poolInfo.getPoolId());
            deRegistration.setPoolName(poolInfo.getPoolName());
            deRegistration.setPoolView(poolInfo.getPoolView());
            deRegistration.setStakeKeys(poolUpdateRepository.findOwnerAccountByPoolView(poolView));
          });
    }
    return deRegistrations;
  }

  @Override
  public List<EpochSize> getPoolSizes(PoolReportHistory poolReportHistory, Pageable pageable) {

    boolean isKoiOs = fetchRewardDataService.isKoiOs();
    if (isKoiOs) {
      Set<String> poolReportSet = Set.of(poolReportHistory.getPoolView());
      boolean isHistory = fetchRewardDataService.checkPoolHistoryForPool(poolReportSet);
      List<PoolHistoryKoiOsProjection> poolHistoryProjections = new ArrayList<>();
      if (!isHistory) {
        boolean isFetch = fetchRewardDataService.fetchPoolHistoryForPool(poolReportSet);
        if (isFetch) {
          poolHistoryProjections =
              poolHistoryRepository.getPoolHistoryKoiOs(
                  poolReportHistory.getPoolView(),
                  poolReportHistory.getBeginEpoch(),
                  poolReportHistory.getEndEpoch());
        }
      } else {
        poolHistoryProjections =
            poolHistoryRepository.getPoolHistoryKoiOs(
                poolReportHistory.getPoolView(),
                poolReportHistory.getBeginEpoch(),
                poolReportHistory.getEndEpoch());
      }

      return poolHistoryProjections.stream().map(EpochSize::toDomain).collect(Collectors.toList());
    } else {
      return epochStakeRepository
          .getEpochSizeByPoolReport(
              poolReportHistory.getPoolView(),
              poolReportHistory.getBeginEpoch(),
              poolReportHistory.getEndEpoch(),
              pageable)
          .getContent()
          .stream()
          .map(EpochSize::toDomain)
          .collect(Collectors.toList());
    }
  }
}
