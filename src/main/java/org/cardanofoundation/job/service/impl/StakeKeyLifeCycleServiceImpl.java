package org.cardanofoundation.job.service.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import io.micrometer.common.util.StringUtils;
import org.cardanofoundation.explorer.consumercommon.entity.StakeAddress;
import org.cardanofoundation.explorer.consumercommon.entity.Tx;
import org.cardanofoundation.job.common.enumeration.TxStatus;
import org.cardanofoundation.job.dto.report.stake.StakeDelegationFilterResponse;
import org.cardanofoundation.job.dto.report.stake.StakeLifeCycleFilterRequest;
import org.cardanofoundation.job.dto.report.stake.StakeRegistrationLifeCycle;
import org.cardanofoundation.job.dto.report.stake.StakeRewardResponse;
import org.cardanofoundation.job.dto.report.stake.StakeWalletActivityResponse;
import org.cardanofoundation.job.dto.report.stake.StakeWithdrawalFilterResponse;
import org.cardanofoundation.job.projection.StakeTxProjection;
import org.cardanofoundation.job.repository.ledgersync.AddressTxBalanceRepository;
import org.cardanofoundation.job.repository.ledgersync.DelegationRepository;
import org.cardanofoundation.job.repository.ledgersync.RewardRepository;
import org.cardanofoundation.job.repository.ledgersync.StakeAddressRepository;
import org.cardanofoundation.job.repository.ledgersync.StakeDeRegistrationRepository;
import org.cardanofoundation.job.repository.ledgersync.StakeRegistrationRepository;
import org.cardanofoundation.job.repository.ledgersync.TxRepository;
import org.cardanofoundation.job.repository.ledgersync.WithdrawalRepository;
import org.cardanofoundation.job.service.FetchRewardDataService;
import org.cardanofoundation.job.service.StakeKeyLifeCycleService;
import org.cardanofoundation.job.util.DataUtil;

@Service
@RequiredArgsConstructor
@Log4j2
public class StakeKeyLifeCycleServiceImpl implements StakeKeyLifeCycleService {

  public static final String MIN_TIME = "1970-01-01 00:00:00";
  private final StakeAddressRepository stakeAddressRepository;
  private final StakeRegistrationRepository stakeRegistrationRepository;
  private final StakeDeRegistrationRepository stakeDeRegistrationRepository;
  private final DelegationRepository delegationRepository;
  private final RewardRepository rewardRepository;
  private final WithdrawalRepository withdrawalRepository;
  private final FetchRewardDataService fetchRewardDataService;
  private final AddressTxBalanceRepository addressTxBalanceRepository;
  private final TxRepository txRepository;

  @Override
  public List<StakeWalletActivityResponse> getStakeWalletActivities(
      String stakeKey, Pageable pageable, StakeLifeCycleFilterRequest condition) {

    makeCondition(condition);

    var txAmountList =
        addressTxBalanceRepository
            .findTxAndAmountByStake(
                stakeKey, condition.getFromDate(), condition.getToDate(), pageable)
            .getContent();

    List<Long> txIds =
        txAmountList.stream().map(StakeTxProjection::getTxId).collect(Collectors.toList());

    /**
     * Due to txAmountList may be very large, we need to split it into sub list then get txList,
     * registrationFutureList, deregistrationFutureList, delegationFutureList, withdrawalFutureList
     * in parallel to reduce the time
     */
    List<CompletableFuture<List<Tx>>> txFutureList = new ArrayList<>();

    int subListSize = 50000;
    for (int i = 0; i < txIds.size(); i += subListSize) {
      List<Long> subTxList = txIds.subList(i, Math.min(txIds.size(), i + subListSize));
      txFutureList.add(CompletableFuture.supplyAsync(() -> txRepository.findByIdIn(subTxList)));
    }

    var txList = txFutureList.stream().map(CompletableFuture::join).flatMap(List::stream).toList();

    Map<Long, Tx> txMap = txList.stream().collect(Collectors.toMap(Tx::getId, Function.identity()));

    return txAmountList.stream()
        .parallel()
        .map(
            item -> {
              StakeWalletActivityResponse stakeWalletActivity = new StakeWalletActivityResponse();
              stakeWalletActivity.setTxHash(txMap.get(item.getTxId()).getHash());
              stakeWalletActivity.setAmount(item.getAmount());
              stakeWalletActivity.setRawAmount(item.getAmount().doubleValue() / 1000000);
              stakeWalletActivity.setTime(item.getTime().toLocalDateTime());
              stakeWalletActivity.setFee(txMap.get(item.getTxId()).getFee());
              if (Boolean.TRUE.equals(txMap.get(item.getTxId()).getValidContract())) {
                stakeWalletActivity.setStatus(TxStatus.SUCCESS);
              } else {
                stakeWalletActivity.setStatus(TxStatus.FAIL);
              }
              return stakeWalletActivity;
            })
        .toList();
  }

  @Override
  public List<StakeRegistrationLifeCycle> getStakeRegistrations(
      String stakeKey, Pageable pageable, StakeLifeCycleFilterRequest condition) {

    StakeAddress stakeAddress = stakeAddressRepository.findByView(stakeKey);
    makeCondition(condition);

    return stakeRegistrationRepository
        .getStakeRegistrationsByAddress(
            stakeAddress, null, condition.getFromDate(), condition.getToDate(), pageable)
        .getContent()
        .stream()
        .map(
            item ->
                StakeRegistrationLifeCycle.builder()
                    .txHash(item.getTxHash())
                    .fee(item.getFee())
                    .deposit(item.getDeposit())
                    .rawFee(item.getFee().doubleValue() / 1000000)
                    .rawDeposit(item.getDeposit().doubleValue() / 1000000)
                    .time(item.getTime().toLocalDateTime())
                    .build())
        .collect(Collectors.toList());
  }

  @Override
  public List<StakeDelegationFilterResponse> getStakeDelegations(
      String stakeKey, Pageable pageable, StakeLifeCycleFilterRequest condition) {
    StakeAddress stakeAddress = stakeAddressRepository.findByView(stakeKey);
    makeCondition(condition);
    return delegationRepository
        .findDelegationByAddress(
            stakeAddress, null, condition.getFromDate(), condition.getToDate(), pageable)
        .getContent()
        .stream()
        .map(
            item ->
                StakeDelegationFilterResponse.builder()
                    .txHash(item.getTxHash())
                    .fee(item.getFee())
                    .time(item.getTime().toLocalDateTime())
                    .outSum(item.getOutSum())
                    .rawFee(item.getFee().doubleValue() / 1000000)
                    .poolName(StringUtils.isEmpty(item.getPoolName()) ? item.getPoolId() : item.getPoolName())
                    .build())
        .collect(Collectors.toList());
  }

  @Override
  public List<StakeRewardResponse> getStakeRewards(
      String stakeKey, Pageable pageable, StakeLifeCycleFilterRequest condition) {
    StakeAddress stakeAddress = stakeAddressRepository.findByView(stakeKey);
    makeCondition(condition);
    fetchReward(stakeKey);

    return rewardRepository
        .findRewardByStake(stakeAddress, condition.getFromDate(), condition.getToDate(), pageable)
        .getContent()
        .stream()
        .map(
            item ->
                StakeRewardResponse.builder()
                    .amount(item.getAmount())
                    .rawAmount(item.getAmount().doubleValue() / 1000000)
                    .epoch(item.getEpoch())
                    .time(item.getTime())
                    .build())
        .collect(Collectors.toList());
  }

  @Override
  public List<StakeWithdrawalFilterResponse> getStakeWithdrawals(
      String stakeKey, Pageable pageable, StakeLifeCycleFilterRequest condition) {
    StakeAddress stakeAddress = stakeAddressRepository.findByView(stakeKey);
    makeCondition(condition);

    return withdrawalRepository
        .getWithdrawalByAddress(
            stakeAddress, null, condition.getFromDate(), condition.getToDate(), pageable)
        .getContent()
        .stream()
        .map(
            item ->
                StakeWithdrawalFilterResponse.builder()
                    .txHash(item.getTxHash())
                    .fee(item.getFee())
                    .time(item.getTime().toLocalDateTime())
                    .value(item.getAmount())
                    .rawValue(item.getAmount().doubleValue() / 1000000)
                    .rawFee(item.getFee().doubleValue() / 1000000)
                    .build())
        .collect(Collectors.toList());
  }

  @Override
  public List<StakeRegistrationLifeCycle> getStakeDeRegistrations(
      String stakeKey, Pageable pageable, StakeLifeCycleFilterRequest condition) {
    StakeAddress stakeAddress = stakeAddressRepository.findByView(stakeKey);
    makeCondition(condition);
    return stakeDeRegistrationRepository
        .getStakeDeRegistrationsByAddress(
            stakeAddress, null, condition.getFromDate(), condition.getToDate(), pageable)
        .getContent()
        .stream()
        .map(
            item ->
                StakeRegistrationLifeCycle.builder()
                    .txHash(item.getTxHash())
                    .fee(item.getFee())
                    .rawFee(item.getFee().doubleValue() / 1000000)
                    .rawDeposit(makePositive(item.getDeposit()).doubleValue() / 1000000)
                    .deposit(makePositive(item.getDeposit()))
                    .time(item.getTime().toLocalDateTime())
                    .build())
        .collect(Collectors.toList());
  }

  private Long makePositive(Long value) {
    return value == null ? null : Math.abs(value);
  }

  private void makeCondition(StakeLifeCycleFilterRequest condition) {
    if (DataUtil.isNullOrEmpty(condition.getFromDate())) {
      condition.setFromDate(Timestamp.valueOf(MIN_TIME));
    }
    if (DataUtil.isNullOrEmpty(condition.getToDate())) {
      condition.setToDate(
          Timestamp.from(
              LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toInstant(ZoneOffset.UTC)));
    }
  }

  private void fetchReward(String stakeKey) {
    if (!fetchRewardDataService.checkRewardAvailable(stakeKey)) {
      boolean fetchRewardResponse =
          fetchRewardDataService.fetchReward(Collections.singleton(stakeKey));
      if (!fetchRewardResponse) {
        throw new RuntimeException("Fetch reward failed");
      }
    }
  }
}
