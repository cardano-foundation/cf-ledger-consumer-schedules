package org.cardanofoundation.job.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.cardanofoundation.explorer.consumercommon.entity.StakeAddress;
import org.cardanofoundation.explorer.consumercommon.entity.Tx;
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
  private final WithdrawalRepository withdrawalRepository;

  @Async
  public CompletableFuture<List<Tx>> findTxByIdIn(List<Long> txIdList) {
    return CompletableFuture.completedFuture(txRepository.findByIdIn(txIdList));
  }

  @Async
  public CompletableFuture<List<Long>> findStakeRegistrationByAddressAndTxIn(
      StakeAddress stakeAddress, List<Long> txIdList) {
    return CompletableFuture.completedFuture(
        stakeRegistrationRepository.getStakeRegistrationsByAddressAndTxIn(stakeAddress, txIdList));
  }

  @Async
  public CompletableFuture<List<Long>> findStakeDeRegistrationByAddressAndTxIn(
      StakeAddress stakeAddress, List<Long> txIdList) {
    return CompletableFuture.completedFuture(
        stakeDeRegistrationRepository.getStakeDeRegistrationsByAddressAndTxIn(
            stakeAddress, txIdList));
  }

  @Async
  public CompletableFuture<List<Long>> findDelegationByAddressAndTxIn(
      StakeAddress stakeAddress, List<Long> txIdList) {
    return CompletableFuture.completedFuture(
        delegationRepository.findDelegationByAddressAndTxIn(stakeAddress, txIdList));
  }

  @Async
  public CompletableFuture<List<Long>> findWithdrawalByAddressAndTxIn(
      StakeAddress stakeAddress, List<Long> txIdList) {
    return CompletableFuture.completedFuture(
        withdrawalRepository.getWithdrawalByAddressAndTxIn(stakeAddress, txIdList));
  }
}
