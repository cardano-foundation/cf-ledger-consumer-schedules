package org.cardanofoundation.job.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.cardanofoundation.job.common.enumeration.PoolActionType;
import org.cardanofoundation.job.dto.PoolCertificateHistory;
import org.cardanofoundation.job.mapper.PoolCertificateMapper;
import org.cardanofoundation.job.projection.PoolCertificateProjection;
import org.cardanofoundation.job.repository.ledgersync.PoolRetireRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolUpdateRepository;
import org.cardanofoundation.job.service.PoolCertificateService;

@Service
@RequiredArgsConstructor
public class PoolCertificateServiceImpl implements PoolCertificateService {

  private final PoolUpdateRepository poolUpdateRepository;
  private final PoolRetireRepository poolRetireRepository;

  private final PoolCertificateMapper poolCertificateMapper;

  @Override
  public List<PoolCertificateHistory> getPoolCertificateByAction(
      String poolViewOrHash, PoolActionType action) {
    return getAllPoolCertificateHistories(poolViewOrHash).stream()
        .filter(poolCertificate -> poolCertificate.getActionType().equals(action))
        .sorted(Comparator.comparing(PoolCertificateHistory::getTxId))
        .toList();
  }

  private List<PoolCertificateHistory> getAllPoolCertificateHistories(String poolViewOrHash) {
    List<PoolCertificateHistory> certificateHistories =
        Stream.concat(
                poolUpdateRepository.getPoolUpdateByPoolViewOrHash(poolViewOrHash).stream(),
                poolRetireRepository.getPoolRetireByPoolViewOrHash(poolViewOrHash).stream())
            .sorted(
                Comparator.comparing(PoolCertificateProjection::getTxId)
                    .thenComparing(PoolCertificateProjection::getCertIndex))
            .map(poolCertificateMapper::fromPoolCertificateProjection)
            .toList();

    for (int i = 0; i < certificateHistories.size(); i++) {
      PoolCertificateHistory certificateHistory = certificateHistories.get(i);
      if (i == 0) {
        certificateHistory.setActionType(PoolActionType.POOL_REGISTRATION);
        continue;
      }
      if (!Objects.isNull(certificateHistory.getPoolRetireId())) {
        certificateHistory.setActionType(PoolActionType.POOL_DEREGISTRATION);
      } else if (!Objects.isNull(certificateHistory.getPoolUpdateId())) {
        PoolCertificateHistory previousCertificateHistory = certificateHistories.get(i - 1);
        if (previousCertificateHistory.getActionType().equals(PoolActionType.POOL_DEREGISTRATION)
            && certificateHistory.getTxEpochNo() >= previousCertificateHistory.getCertEpochNo()) {
          certificateHistory.setActionType(PoolActionType.POOL_REGISTRATION);
        } else {
          certificateHistory.setActionType(PoolActionType.POOL_UPDATE);
        }
      }
    }

    return certificateHistories;
  }
}
