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

import org.cardanofoundation.explorer.consumercommon.entity.PoolUpdate;
import org.cardanofoundation.job.dto.report.pool.DeRegistrationResponse;
import org.cardanofoundation.job.dto.report.pool.PoolUpdateDetailResponse;
import org.cardanofoundation.job.dto.report.pool.RewardResponse;
import org.cardanofoundation.job.dto.report.pool.TabularRegisResponse;
import org.cardanofoundation.job.projection.EpochRewardProjection;
import org.cardanofoundation.job.projection.LifeCycleRewardProjection;
import org.cardanofoundation.job.projection.PoolDeRegistrationProjection;
import org.cardanofoundation.job.projection.PoolInfoProjection;
import org.cardanofoundation.job.projection.PoolRegistrationProjection;
import org.cardanofoundation.job.projection.PoolUpdateDetailProjection;
import org.cardanofoundation.job.projection.StakeKeyProjection;
import org.cardanofoundation.job.repository.PoolHashRepository;
import org.cardanofoundation.job.repository.PoolRetireRepository;
import org.cardanofoundation.job.repository.PoolUpdateRepository;
import org.cardanofoundation.job.repository.RewardRepository;
import org.cardanofoundation.job.service.PoolLifecycleService;

@Service
@Log4j2
@RequiredArgsConstructor
public class PoolLifecycleServiceImpl implements PoolLifecycleService {

  private final PoolHashRepository poolHashRepository;
  private final PoolUpdateRepository poolUpdateRepository;
  private final RewardRepository rewardRepository;
  private final PoolRetireRepository poolRetireRepository;

  @Override
  public List<TabularRegisResponse> registrationList(String poolView, Pageable pageable) {
    List<TabularRegisResponse> tabularRegisList = new ArrayList<>();
    Page<PoolRegistrationProjection> projection = poolHashRepository.getPoolRegistrationByPool(
        poolView, pageable);
    if (Objects.nonNull(projection)) {
      Set<Long> poolUpdateIds = new HashSet<>();
      projection.stream().forEach(tabularRegis -> {
        tabularRegisList.add(new TabularRegisResponse(tabularRegis));
        poolUpdateIds.add(tabularRegis.getPoolUpdateId());
      });
      List<StakeKeyProjection> stakeKeyProjections = poolUpdateRepository
          .findOwnerAccountByPoolUpdate(poolUpdateIds);
      Map<Long, List<StakeKeyProjection>> stakeKeyProjectionMap = stakeKeyProjections
          .stream().collect(Collectors.groupingBy(StakeKeyProjection::getPoolUpdateId));
      Map<Long, List<String>> stakeKeyStrMap = new HashMap<>();
      stakeKeyProjectionMap
          .forEach((k, v) -> stakeKeyStrMap.put(k, v.stream()
              .map(StakeKeyProjection::getView)
              .collect(Collectors.toList())));
      tabularRegisList.forEach(tabularRegis -> tabularRegis.setStakeKeys(
          stakeKeyStrMap.get(tabularRegis.getPoolUpdateId())));
    }
    return tabularRegisList;
  }

  @Override
  public List<PoolUpdateDetailResponse> poolUpdateList(String poolView, Pageable pageable) {
    List<PoolUpdateDetailResponse> poolUpdateList = new ArrayList<>();
    Page<PoolUpdateDetailProjection> projection = poolUpdateRepository.findPoolUpdateByPool(
        poolView, pageable);
    if (Objects.nonNull(projection)) {
      projection.stream().forEach(poolUpdate -> {
        PoolUpdateDetailResponse poolUpdateRes = new PoolUpdateDetailResponse(poolUpdate);
        poolUpdateRes.setStakeKeys(
            poolUpdateRepository.findOwnerAccountByPoolUpdate(poolUpdate.getPoolUpdateId()));
        PoolUpdate poolUpdatePrevious = poolUpdateRepository.findTopByIdLessThanAndPoolHashIdOrderByIdDesc(
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
  public List<RewardResponse> listReward(String poolView, Pageable pageable) {
    List<RewardResponse> rewardRes = new ArrayList<>();
    Page<LifeCycleRewardProjection> projections = rewardRepository.getRewardInfoByPool(poolView,
                                                                                       pageable);
    if (Objects.nonNull(projections)) {
      projections.stream().forEach(projection -> {
        RewardResponse reward = new RewardResponse(projection);
        rewardRes.add(reward);
      });
    }
    return rewardRes;
  }

  @Override
  public List<DeRegistrationResponse> deRegistration(String poolView, Pageable pageable) {
    PoolInfoProjection poolInfo = poolHashRepository.getPoolInfo(poolView);

    Page<PoolDeRegistrationProjection> projections = poolRetireRepository.getPoolDeRegistration(
        poolView, pageable);
    List<DeRegistrationResponse> deRegistrations = new ArrayList<>();
    if (Objects.nonNull(projections)) {
      Set<Integer> epochNos = new HashSet<>();
      projections.stream().forEach(projection -> {
        DeRegistrationResponse deRegistrationRes = new DeRegistrationResponse(projection);
        deRegistrations.add(deRegistrationRes);
        epochNos.add(projection.getRetiringEpoch());
      });
      List<EpochRewardProjection> epochRewardProjections = rewardRepository.getRewardRefundByEpoch(
          poolView, epochNos);
      Map<Integer, BigInteger> refundAmountMap = new HashMap<>();
      epochRewardProjections.forEach(
          refund -> refundAmountMap.put(refund.getEpochNo(), refund.getAmount()));
      deRegistrations.forEach(deRegistration -> {
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
}