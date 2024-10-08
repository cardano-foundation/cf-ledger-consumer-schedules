package org.cardanofoundation.job.service.impl;

import java.math.BigInteger;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import io.micrometer.common.util.StringUtils;

import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.explorer.common.entity.ledgersync.EpochParam;
import org.cardanofoundation.explorer.common.entity.ledgersync.StakeAddress;
import org.cardanofoundation.explorer.common.entity.ledgersync.Tx;
import org.cardanofoundation.job.common.enumeration.TxStatus;
import org.cardanofoundation.job.dto.report.stake.StakeDelegationFilterResponse;
import org.cardanofoundation.job.dto.report.stake.StakeLifeCycleFilterRequest;
import org.cardanofoundation.job.dto.report.stake.StakeRegistrationLifeCycle;
import org.cardanofoundation.job.dto.report.stake.StakeRewardResponse;
import org.cardanofoundation.job.dto.report.stake.StakeWalletActivityResponse;
import org.cardanofoundation.job.dto.report.stake.StakeWithdrawalFilterResponse;
import org.cardanofoundation.job.projection.StakeHistoryProjection;
import org.cardanofoundation.job.projection.StakeTxProjection;
import org.cardanofoundation.job.repository.ledgersync.DelegationRepository;
import org.cardanofoundation.job.repository.ledgersync.EpochParamRepository;
import org.cardanofoundation.job.repository.ledgersync.RewardRepository;
import org.cardanofoundation.job.repository.ledgersync.StakeAddressRepository;
import org.cardanofoundation.job.repository.ledgersync.StakeDeRegistrationRepository;
import org.cardanofoundation.job.repository.ledgersync.StakeRegistrationRepository;
import org.cardanofoundation.job.repository.ledgersync.TxRepository;
import org.cardanofoundation.job.repository.ledgersync.WithdrawalRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.service.FetchRewardDataService;
import org.cardanofoundation.job.service.StakeKeyLifeCycleService;
import org.cardanofoundation.job.util.DataUtil;
import org.cardanofoundation.job.util.DateUtils;

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
  private final AddressTxAmountRepository addressTxAmountRepository;
  private final TxRepository txRepository;
  private final EpochParamRepository epochParamRepository;
  private final CardanoConverters cardanoConverters;

  @Override
  public List<StakeWalletActivityResponse> getStakeWalletActivities(
      String stakeKey, Pageable pageable, StakeLifeCycleFilterRequest condition) {

    makeCondition(condition);

    long slotFrom = cardanoConverters.time().toSlot(condition.getFromDate().toLocalDateTime());
    long slotTo = cardanoConverters.time().toSlot(condition.getToDate().toLocalDateTime());

    var txAmountList =
        addressTxAmountRepository
            .findTxAndAmountByStakeAndSlotRange(stakeKey, slotFrom, slotTo, pageable)
            .getContent();

    List<String> txHashes =
        txAmountList.stream().map(StakeTxProjection::getTxHash).collect(Collectors.toList());

    /**
     * Due to txAmountList may be very large, we need to split it into sub list then get txList,
     * registrationFutureList, deregistrationFutureList, delegationFutureList, withdrawalFutureList
     * in parallel to reduce the time
     */
    List<CompletableFuture<List<Tx>>> txFutureList = new ArrayList<>();

    int subListSize = 50000;
    for (int i = 0; i < txHashes.size(); i += subListSize) {
      List<String> subTxList = txHashes.subList(i, Math.min(txHashes.size(), i + subListSize));
      txFutureList.add(CompletableFuture.supplyAsync(() -> txRepository.findByHashIn(subTxList)));
    }

    var txList = txFutureList.stream().map(CompletableFuture::join).flatMap(List::stream).toList();

    Map<String, Tx> txMap =
        txList.stream().collect(Collectors.toMap(Tx::getHash, Function.identity()));

    return txAmountList.stream()
        .parallel()
        .map(
            item -> {
              StakeWalletActivityResponse stakeWalletActivity = new StakeWalletActivityResponse();
              stakeWalletActivity.setTxHash(item.getTxHash());
              stakeWalletActivity.setAmount(item.getAmount());
              stakeWalletActivity.setRawAmount(item.getAmount().doubleValue() / 1000000);
              stakeWalletActivity.setTime(DateUtils.epochSecondToLocalDateTime(item.getTime()));
              stakeWalletActivity.setFee(txMap.get(item.getTxHash()).getFee());
              if (Boolean.TRUE.equals(txMap.get(item.getTxHash()).getValidContract())) {
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

    Page<StakeHistoryProjection> stakeHistoryList =
        stakeRegistrationRepository.getStakeRegistrationsByAddress(
            stakeAddress, null, condition.getFromDate(), condition.getToDate(), pageable);

    var epochNoList = stakeHistoryList.stream().map(StakeHistoryProjection::getEpochNo).toList();
    var epochParams = epochParamRepository.findByEpochNoIn(epochNoList);
    Map<Integer, BigInteger> epochNoDepositMap =
        epochParams.stream()
            .collect(Collectors.toMap(EpochParam::getEpochNo, EpochParam::getKeyDeposit));

    return stakeHistoryList.stream()
        .map(
            item ->
                StakeRegistrationLifeCycle.builder()
                    .txHash(item.getTxHash())
                    .fee(item.getFee())
                    .deposit(epochNoDepositMap.get(item.getEpochNo()).longValue())
                    .rawFee(item.getFee().doubleValue() / 1000000)
                    .rawDeposit(epochNoDepositMap.get(item.getEpochNo()).doubleValue() / 1000000)
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
                    .poolName(
                        StringUtils.isEmpty(item.getPoolName())
                            ? item.getPoolId()
                            : item.getPoolName())
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

    Page<StakeHistoryProjection> stakeHistoryList =
        stakeDeRegistrationRepository.getStakeDeRegistrationsByAddress(
            stakeAddress, null, condition.getFromDate(), condition.getToDate(), pageable);
    var epochNoList = stakeHistoryList.stream().map(StakeHistoryProjection::getEpochNo).toList();
    var epochParams = epochParamRepository.findByEpochNoIn(epochNoList);
    Map<Integer, BigInteger> epochNoDepositMap =
        epochParams.stream()
            .collect(Collectors.toMap(EpochParam::getEpochNo, EpochParam::getKeyDeposit));

    return stakeHistoryList.stream()
        .map(
            item ->
                StakeRegistrationLifeCycle.builder()
                    .txHash(item.getTxHash())
                    .fee(item.getFee())
                    .rawFee(item.getFee().doubleValue() / 1000000)
                    .deposit(makePositive(epochNoDepositMap.get(item.getEpochNo()).longValue()))
                    .rawDeposit(
                        makePositive(epochNoDepositMap.get(item.getEpochNo()).longValue())
                                .doubleValue()
                            / 1000000)
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
