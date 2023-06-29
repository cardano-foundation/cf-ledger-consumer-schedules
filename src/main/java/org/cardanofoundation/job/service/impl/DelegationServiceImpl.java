package org.cardanofoundation.job.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.job.repository.DelegationRepository;
import org.cardanofoundation.job.service.DelegationService;
import org.cardanofoundation.job.service.PoolService;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DelegationServiceImpl implements DelegationService {

    PoolService poolService;
    DelegationRepository delegationRepository;
    @Override
    public Integer countCurrentDelegator() {
        var poolStatus = poolService.getCurrentPoolStatus();
        return delegationRepository.countCurrentDelegator(poolStatus.getPoolActivateIds());
    }
}
