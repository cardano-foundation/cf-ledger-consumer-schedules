package org.cardanofoundation.job.service.impl;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import org.cardanofoundation.job.dto.PoolStatus;
import org.cardanofoundation.job.projection.PoolUpdateTxProjection;
import org.cardanofoundation.job.repository.EpochRepository;
import org.cardanofoundation.job.repository.PoolRetireRepository;
import org.cardanofoundation.job.repository.PoolUpdateRepository;
import org.cardanofoundation.job.service.PoolService;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PoolServiceImpl implements PoolService {

  EpochRepository epochRepository;

  PoolUpdateRepository poolUpdateRepository;

  PoolRetireRepository poolRetireRepository;

  @Override
  public PoolStatus getCurrentPoolStatus() {
    var currentEpoch = epochRepository.findMaxEpochNo();
    if(Objects.isNull(currentEpoch)){
      return PoolStatus.builder()
          .poolActivateIds(new HashSet<>())
          .poolInactivateIds(new HashSet<>())
          .build();
    }
    var poolCertAndTxId = poolUpdateRepository.findLastPoolCertificate();
    var mPoolRetireCertificate =
        poolRetireRepository.getLastPoolRetireTilEpoch(currentEpoch).stream()
            .collect(Collectors.toMap(PoolUpdateTxProjection::getPoolId, Function.identity()));
    Set<Long> poolActivateIds = new HashSet<>();
    poolCertAndTxId.forEach(
        poolUpdateTxProjection -> {
          var poolRetiredCert = mPoolRetireCertificate.get(poolUpdateTxProjection.getPoolId());
          if (Objects.nonNull(poolRetiredCert)) {
            // if retire and update in the same tx, compare certificate index
            // which one is greater than it will be applied
            if (poolUpdateTxProjection.getTxId().equals(poolRetiredCert.getTxId())) {
              if (poolRetiredCert.getCertificateIndex()
                  < poolUpdateTxProjection.getCertificateIndex()) {
                poolActivateIds.add(poolUpdateTxProjection.getPoolId());
              }
            } else if (poolUpdateTxProjection.getTxId() > poolRetiredCert.getTxId()) {
              poolActivateIds.add(poolUpdateTxProjection.getPoolId());
            }
          } else {
            poolActivateIds.add(poolUpdateTxProjection.getPoolId());
          }
        });

    Set<Long> poolInactivateIds =
        poolCertAndTxId.stream().map(PoolUpdateTxProjection::getPoolId).collect(Collectors.toSet());
    poolInactivateIds.removeAll(poolActivateIds);
    return PoolStatus.builder()
        .poolActivateIds(poolActivateIds)
        .poolInactivateIds(poolInactivateIds)
        .build();
  }
}
