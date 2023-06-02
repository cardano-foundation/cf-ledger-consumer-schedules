package org.cardanofoundation.job.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import org.cardanofoundation.job.dto.report.stake.StakeDelegationFilterResponse;
import org.cardanofoundation.job.dto.report.stake.StakeLifeCycleFilterRequest;
import org.cardanofoundation.job.dto.report.stake.StakeRegistrationLifeCycle;
import org.cardanofoundation.job.dto.report.stake.StakeRewardResponse;
import org.cardanofoundation.job.dto.report.stake.StakeWalletActivityResponse;
import org.cardanofoundation.job.dto.report.stake.StakeWithdrawalFilterResponse;

public interface StakeKeyLifeCycleService {

  /**
   * Get stake wallet ctivities
   *
   * @param stakeKey stake key
   * @param pageable pageable
   * @param condition condition
   * @return stake wallet activities
   */
  List<StakeWalletActivityResponse> getStakeWalletActivities(
      String stakeKey, Pageable pageable, StakeLifeCycleFilterRequest condition);

  /**
   * Get stake registrations
   *
   * @param stakeKey stake key
   * @param pageable pageable
   * @param condition condition
   * @return stake registrations
   */
  List<StakeRegistrationLifeCycle> getStakeRegistrations(
      String stakeKey, Pageable pageable, StakeLifeCycleFilterRequest condition);

  /**
   * Get stake delegations
   *
   * @param stakeKey stake key
   * @param pageable pageable
   * @param condition condition
   * @return stake delegations
   */
  List<StakeDelegationFilterResponse> getStakeDelegations(
      String stakeKey, Pageable pageable, StakeLifeCycleFilterRequest condition);

  /**
   * Get stake rewards
   *
   * @param stakeKey stake key
   * @param pageable pageable
   * @param condition condition
   * @return stake rewards
   */
  List<StakeRewardResponse> getStakeRewards(
      String stakeKey, Pageable pageable, StakeLifeCycleFilterRequest condition);

  /**
   * Get stake withdrawals
   *
   * @param stakeKey stake key
   * @param pageable pageable
   * @param condition condition
   * @return stake withdrawals
   */
  List<StakeWithdrawalFilterResponse> getStakeWithdrawals(
      String stakeKey, Pageable pageable, StakeLifeCycleFilterRequest condition);

  /**
   * Get stake de-registrations
   *
   * @param stakeKey stake key
   * @param pageable pageable
   * @param condition condition
   * @return stake de-registrations
   */
  List<StakeRegistrationLifeCycle> getStakeDeRegistrations(
      String stakeKey, Pageable pageable, StakeLifeCycleFilterRequest condition);
}
