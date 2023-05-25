package org.cardanofoundation.job.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.cardanofoundation.explorer.consumercommon.entity.StakeAddress;
import org.cardanofoundation.explorer.consumercommon.entity.Tx;
import org.cardanofoundation.job.dto.report.stake.StakeLifeCycleFilterRequest;
import org.cardanofoundation.job.projection.StakeTxProjection;
import org.cardanofoundation.job.repository.AddressTxBalanceRepository;
import org.cardanofoundation.job.repository.DelegationRepository;
import org.cardanofoundation.job.repository.StakeDeRegistrationRepository;
import org.cardanofoundation.job.repository.StakeRegistrationRepository;
import org.cardanofoundation.job.repository.TxRepository;
import org.cardanofoundation.job.repository.WithdrawalRepository;

@Service
@RequiredArgsConstructor
@Log4j2
public class StakeKeyLifeCycleServiceAsync {

  private final TxRepository txRepository;
  private final StakeRegistrationRepository stakeRegistrationRepository;
  private final StakeDeRegistrationRepository stakeDeRegistrationRepository;
  private final DelegationRepository delegationRepository;
  private final AddressTxBalanceRepository addressTxBalanceRepository;
  private final WithdrawalRepository withdrawalRepository;

  @Async
  public CompletableFuture<List<Tx>> findTxByIdIn(List<Long> txIdList) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    return CompletableFuture.completedFuture(txRepository.findByIdIn(txIdList));
  }

  @Async
  public CompletableFuture<List<Long>> findStakeRegistrationByAddressAndTxIn(
      StakeAddress stakeAddress, List<Long> txIdList) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    return CompletableFuture.completedFuture(
        stakeRegistrationRepository.getStakeRegistrationsByAddressAndTxIn(stakeAddress, txIdList));
  }

  @Async
  public CompletableFuture<List<Long>> findStakeDeRegistrationByAddressAndTxIn(
      StakeAddress stakeAddress, List<Long> txIdList) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    return CompletableFuture.completedFuture(
        stakeDeRegistrationRepository.getStakeDeRegistrationsByAddressAndTxIn(stakeAddress,
                                                                              txIdList));
  }

  @Async
  public CompletableFuture<List<Long>> findDelegationByAddressAndTxIn(
      StakeAddress stakeAddress, List<Long> txIdList) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    return CompletableFuture.completedFuture(
        delegationRepository.findDelegationByAddressAndTxIn(stakeAddress, txIdList));
  }

  @Async
  public CompletableFuture<List<StakeTxProjection>> findTxAndAmountByStake(String view,
                                                                           Pageable pageable,
                                                                           StakeLifeCycleFilterRequest condition) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    return CompletableFuture.completedFuture(
        addressTxBalanceRepository.findTxAndAmountByStake(view, condition.getFromDate(),
                                                          condition.getToDate(), pageable)
            .getContent());
  }

  @Async
  public CompletableFuture<List<Long>> findWithdrawalByAddressAndTxIn(StakeAddress stakeAddress, List<Long> txIdList) {
    log.info("Execute method {} asynchronously with thread {}",
             Thread.currentThread().getStackTrace()[1].getMethodName(),
             Thread.currentThread().getName());
    return CompletableFuture.completedFuture(
        withdrawalRepository.getWithdrawalByAddressAndTxIn(stakeAddress, txIdList));
  }
}
