package org.cardanofoundation.job.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.job.dto.PoolStatus;
import org.cardanofoundation.job.projection.PoolUpdateTxProjection;
import org.cardanofoundation.job.repository.EpochRepository;
import org.cardanofoundation.job.repository.PoolRetireRepository;
import org.cardanofoundation.job.repository.PoolUpdateRepository;
import org.cardanofoundation.job.service.PoolService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class PoolServiceImpl implements PoolService {

    EpochRepository epochRepository;

    PoolUpdateRepository poolUpdateRepository;

    PoolRetireRepository poolRetireRepository;


    @Override
    public PoolStatus getCurrentPoolStatus() {
        int currentEpoch = epochRepository.findMaxEpochNo();
        var poolCertAndTxId = poolUpdateRepository.findLastPoolCertificate();
        var mPoolRetireCertificate = poolRetireRepository.getLastPoolRetireTilEpoch(currentEpoch).stream()
                .collect(Collectors.toMap(PoolUpdateTxProjection::getPoolId, PoolUpdateTxProjection::getTxId));
        Set<Long> poolActivateIds = new HashSet<>();
        poolCertAndTxId.forEach(poolUpdateTxProjection -> {
            if (!mPoolRetireCertificate.containsKey(poolUpdateTxProjection.getPoolId())
                    || poolUpdateTxProjection.getTxId() > mPoolRetireCertificate.get(poolUpdateTxProjection.getPoolId())) {
                poolActivateIds.add(poolUpdateTxProjection.getPoolId());
            }
        });

        Set<Long> poolInactivateIds = poolCertAndTxId.stream().map(PoolUpdateTxProjection::getPoolId).collect(Collectors.toSet());
        poolInactivateIds.removeAll(poolActivateIds);
        return PoolStatus.builder().poolActivateIds(poolActivateIds)
                .poolInactivateIds(poolInactivateIds).build();

    }
}
