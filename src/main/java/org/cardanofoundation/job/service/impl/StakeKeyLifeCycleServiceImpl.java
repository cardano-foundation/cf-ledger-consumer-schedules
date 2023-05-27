package org.cardanofoundation.job.service.impl;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.cardanofoundation.explorer.consumercommon.entity.StakeAddress;
import org.cardanofoundation.explorer.consumercommon.entity.Tx;
import org.cardanofoundation.job.common.enumeration.StakeTxType;
import org.cardanofoundation.job.common.enumeration.TxStatus;
import org.cardanofoundation.job.dto.report.stake.StakeDelegationFilterResponse;
import org.cardanofoundation.job.dto.report.stake.StakeLifeCycleFilterRequest;
import org.cardanofoundation.job.dto.report.stake.StakeRegistrationLifeCycle;
import org.cardanofoundation.job.dto.report.stake.StakeRewardResponse;
import org.cardanofoundation.job.dto.report.stake.StakeWalletActivityResponse;
import org.cardanofoundation.job.dto.report.stake.StakeWithdrawalFilterResponse;
import org.cardanofoundation.job.projection.StakeTxProjection;
import org.cardanofoundation.job.repository.DelegationRepository;
import org.cardanofoundation.job.repository.RewardRepository;
import org.cardanofoundation.job.repository.StakeAddressRepository;
import org.cardanofoundation.job.repository.StakeDeRegistrationRepository;
import org.cardanofoundation.job.repository.StakeRegistrationRepository;
import org.cardanofoundation.job.repository.WithdrawalRepository;
import org.cardanofoundation.job.service.StakeKeyLifeCycleService;
import org.cardanofoundation.job.service.StakeKeyLifeCycleServiceAsync;
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

  private final StakeKeyLifeCycleServiceAsync asyncService;

  @Override
  public List<StakeWalletActivityResponse> getStakeWalletActivities(String stakeKey,
                                                                    Pageable pageable,
                                                                    StakeLifeCycleFilterRequest condition) {

    StakeAddress stakeAddress = stakeAddressRepository.findByView(stakeKey);
    makeCondition(condition);

    List<CompletableFuture<List<StakeTxProjection>>> txAmountListFuture = new ArrayList<>();

    int subPageSize = 2000;
    int pagesize = pageable.getPageSize();

    for (int i = 0; ; i++) {
      if (pagesize <= subPageSize) {
        Pageable subPageable = PageRequest.of(i, pagesize);
        txAmountListFuture.add(
            asyncService.findTxAndAmountByStake(stakeAddress.getView(), subPageable, condition));
        break;
      }
      Pageable subPageable = PageRequest.of(i, subPageSize);
      txAmountListFuture.add(
          asyncService.findTxAndAmountByStake(stakeAddress.getView(), subPageable, condition));
      pagesize -= subPageSize;
    }

    var txAmountList = txAmountListFuture.stream().map(CompletableFuture::join)
        .flatMap(List::stream).toList();

    List<Long> txIds = txAmountList.stream().map(StakeTxProjection::getTxId)
        .collect(Collectors.toList());

    List<CompletableFuture<List<Tx>>> txFutureList = new ArrayList<>();
    List<CompletableFuture<List<Long>>> registrationFutureList = new ArrayList<>();
    List<CompletableFuture<List<Long>>> deregistrationFutureList = new ArrayList<>();
    List<CompletableFuture<List<Long>>> delegationFutureList = new ArrayList<>();
    List<CompletableFuture<List<Long>>> withdrawalFutureList = new ArrayList<>();

    int subListSize = 2000;
    for (int i = 0; i < txIds.size(); i += subListSize) {
      List<Long> subTxList = txIds.subList(i, Math.min(txIds.size(), i + subListSize));
      txFutureList.add(asyncService.findTxByIdIn(subTxList));
      registrationFutureList.add(
          asyncService.findStakeRegistrationByAddressAndTxIn(stakeAddress, subTxList));
      deregistrationFutureList.add(
          asyncService.findStakeDeRegistrationByAddressAndTxIn(stakeAddress, subTxList));
      delegationFutureList.add(
          asyncService.findDelegationByAddressAndTxIn(stakeAddress, subTxList));
      withdrawalFutureList.add(
          asyncService.findWithdrawalByAddressAndTxIn(stakeAddress, subTxList));
    }


    var txList = txFutureList.stream().map(CompletableFuture::join).flatMap(List::stream).toList();
    var registrationList = registrationFutureList.stream().map(CompletableFuture::join)
        .flatMap(List::stream).toList();
    var deregistrationList = deregistrationFutureList.stream().map(CompletableFuture::join)
        .flatMap(List::stream).toList();
    var delegationList = delegationFutureList.stream().map(CompletableFuture::join)
        .flatMap(List::stream).toList();
    var withdrawalList = withdrawalFutureList.stream().map(CompletableFuture::join)
        .flatMap(List::stream).toList();

    Map<Long, Tx> txMap = txList.stream().collect(Collectors.toMap(Tx::getId, Function.identity()));

    return txAmountList.stream().map(item -> {
      StakeWalletActivityResponse stakeWalletActivity = new StakeWalletActivityResponse();
      stakeWalletActivity.setTxHash(txMap.get(item.getTxId()).getHash());
      stakeWalletActivity.setAmount(item.getAmount());
      stakeWalletActivity.setTime(item.getTime().toLocalDateTime());
      stakeWalletActivity.setFee(txMap.get(item.getTxId()).getFee());
      if (Boolean.TRUE.equals(txMap.get(item.getTxId()).getValidContract())) {
        stakeWalletActivity.setStatus(TxStatus.SUCCESS);
      } else {
        stakeWalletActivity.setStatus(TxStatus.FAIL);
      }
      stakeWalletActivity.setType(getStakeTxType(stakeWalletActivity, txMap.get(item.getTxId()),
                                                 registrationList, deregistrationList,
                                                 delegationList, withdrawalList));
      return stakeWalletActivity;
    }).toList();
  }


  @Override
  public List<StakeRegistrationLifeCycle> getStakeRegistrations(String stakeKey, Pageable pageable,
                                                                StakeLifeCycleFilterRequest condition) {

    StakeAddress stakeAddress = stakeAddressRepository.findByView(stakeKey);
    makeCondition(condition);

    return stakeRegistrationRepository
        .getStakeRegistrationsByAddress(stakeAddress, null, condition.getFromDate(),
                                        condition.getToDate(), pageable)
        .getContent()
        .stream()
        .map(item -> StakeRegistrationLifeCycle.builder()
            .txHash(item.getTxHash())
            .fee(item.getFee())
            .deposit(item.getDeposit())
            .time(item.getTime().toLocalDateTime())
            .build()
        ).collect(Collectors.toList());
  }

  @Override
  public List<StakeDelegationFilterResponse> getStakeDelegations(String stakeKey, Pageable pageable,
                                                                 StakeLifeCycleFilterRequest condition) {
    StakeAddress stakeAddress = stakeAddressRepository.findByView(stakeKey);
    makeCondition(condition);
    return delegationRepository
        .findDelegationByAddress(stakeAddress, null, condition.getFromDate(), condition.getToDate(),
                                 pageable)
        .getContent()
        .stream()
        .map(item -> StakeDelegationFilterResponse.builder()
            .txHash(item.getTxHash())
            .fee(item.getFee())
            .time(item.getTime().toLocalDateTime())
            .outSum(item.getOutSum())
            .build()
        ).collect(Collectors.toList());
  }

  @Override
  public List<StakeRewardResponse> getStakeRewards(String stakeKey, Pageable pageable,
                                                   StakeLifeCycleFilterRequest condition) {
    StakeAddress stakeAddress = stakeAddressRepository.findByView(stakeKey);
    makeCondition(condition);

    return rewardRepository.findRewardByStake(stakeAddress, condition.getFromDate(),
                                              condition.getToDate(), pageable).getContent();
  }

  @Override
  public List<StakeWithdrawalFilterResponse> getStakeWithdrawals(String stakeKey, Pageable pageable,
                                                                 StakeLifeCycleFilterRequest condition) {
    StakeAddress stakeAddress = stakeAddressRepository.findByView(stakeKey);
    makeCondition(condition);

    return withdrawalRepository.getWithdrawalByAddress(stakeAddress, null, condition.getFromDate(),
                                                       condition.getToDate(), pageable)
        .getContent()
        .stream()
        .map(item -> StakeWithdrawalFilterResponse.builder()
            .txHash(item.getTxHash())
            .fee(item.getFee())
            .time(item.getTime().toLocalDateTime())
            .value(item.getAmount())
            .build()
        ).collect(Collectors.toList());
  }

  @Override
  public List<StakeRegistrationLifeCycle> getStakeDeRegistrations(String stakeKey,
                                                                  Pageable pageable,
                                                                  StakeLifeCycleFilterRequest condition) {
    StakeAddress stakeAddress = stakeAddressRepository.findByView(stakeKey);
    makeCondition(condition);
    return stakeDeRegistrationRepository
        .getStakeDeRegistrationsByAddress(stakeAddress, null, condition.getFromDate(),
                                          condition.getToDate(), pageable)
        .getContent()
        .stream()
        .map(item -> StakeRegistrationLifeCycle.builder()
            .txHash(item.getTxHash())
            .fee(item.getFee())
            .deposit(makePositive(item.getDeposit()))
            .time(item.getTime().toLocalDateTime())
            .build()
        ).collect(Collectors.toList());
  }

  private Long makePositive(Long value) {
    return value == null ? null : Math.abs(value);
  }

  private String getStakeTxType(StakeWalletActivityResponse stakeWalletActivity, Tx tx,
                                     List<Long> registrationList, List<Long> deregistrationList, List<Long> delegationList, List<Long> withdrawList) {
    boolean isRegistration = registrationList.contains(tx.getId());
    boolean isDeRegistration = deregistrationList.contains(tx.getId());
    boolean isDelegation = delegationList.contains(tx.getId());
    boolean isWithdraw = withdrawList.contains(tx.getId());
    BigInteger fee = tx.getFee();
    BigInteger amount = stakeWalletActivity.getAmount();
    Long deposit = tx.getDeposit();
    if(deposit != null && deposit != 0 && (isRegistration || isDeRegistration)) {
      if(isRegistration) {
        return StakeTxType.CERTIFICATE_HOLD_PAID.getValue();
      }
      else {
        return StakeTxType.CERTIFICATE_HOLD_DEPOSIT_REFUNDED.getValue();
      }
    } else if(fee != null && fee.abs().compareTo(stakeWalletActivity.getAmount().abs()) == 0) {
      if (isRegistration || isDeRegistration || isDelegation) {
        return StakeTxType.CERTIFICATE_FEE_PAID.getValue();
      } else {
        return StakeTxType.FEE_PAID.getValue();
      }
    } else if(amount != null && amount.compareTo(BigInteger.ZERO) < 0) {
      return StakeTxType.SENT.getValue();
    } else if (isWithdraw) {
      return StakeTxType.REWARD_WITHDRAWN.getValue();
    }
    else {
      return StakeTxType.RECEIVED.getValue();
    }
  }

  private void makeCondition(StakeLifeCycleFilterRequest condition) {
    if (DataUtil.isNullOrEmpty(condition.getFromDate())) {
      condition.setFromDate(Timestamp.valueOf(MIN_TIME));
    }
    if (DataUtil.isNullOrEmpty(condition.getToDate())) {
      condition.setToDate(Timestamp.from(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                                             .toInstant(ZoneOffset.UTC)));
    }
  }
}
