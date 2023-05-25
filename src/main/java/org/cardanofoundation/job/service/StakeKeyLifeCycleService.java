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

  List<StakeWalletActivityResponse> getStakeWalletActivities(String stakeKey, Pageable pageable,
                                                                                StakeLifeCycleFilterRequest condition);

  List<StakeRegistrationLifeCycle> getStakeRegistrations(String stakeKey, Pageable pageable,
                                                         StakeLifeCycleFilterRequest condition);


  List<StakeDelegationFilterResponse> getStakeDelegations(String stakeKey, Pageable pageable,
                                                          StakeLifeCycleFilterRequest condition);

  List<StakeRewardResponse> getStakeRewards(String stakeKey, Pageable pageable,
                                            StakeLifeCycleFilterRequest condition);

  List<StakeWithdrawalFilterResponse> getStakeWithdrawals(String stakeKey, Pageable pageable,
                                                          StakeLifeCycleFilterRequest condition);

  List<StakeRegistrationLifeCycle> getStakeDeRegistrations(String stakeKey, Pageable pageable,
                                                           StakeLifeCycleFilterRequest condition);
}
