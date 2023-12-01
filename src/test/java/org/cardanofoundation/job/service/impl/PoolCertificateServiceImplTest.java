package org.cardanofoundation.job.service.impl;

import java.sql.Timestamp;
import java.util.List;

import org.cardanofoundation.job.common.enumeration.PoolActionType;
import org.cardanofoundation.job.mapper.PoolCertificateMapperImpl;
import org.cardanofoundation.job.projection.PoolCertificateProjectionImpl;
import org.cardanofoundation.job.repository.ledgersync.PoolRetireRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolUpdateRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class PoolCertificateServiceImplTest {

  @Mock
  PoolUpdateRepository poolUpdateRepository;
  @Mock
  PoolRetireRepository poolRetireRepository;

  PoolCertificateServiceImpl poolCertificateService;

  @BeforeEach
  void setUp() {
    poolCertificateService = new PoolCertificateServiceImpl(poolUpdateRepository,
                                                            poolRetireRepository,
                                                            new PoolCertificateMapperImpl());
  }

  @Test
  void getPoolCertificateByAction_shouldReturnCertificateRegistrationList() {
    PoolCertificateProjectionImpl certificateUpdate1 = PoolCertificateProjectionImpl.builder()
        .txId(1L).txEpochNo(5).certEpochNo(8).certIndex(0).poolUpdateId(1L)
        .blockTime(Timestamp.valueOf("2021-01-01 00:00:00")).build();

    PoolCertificateProjectionImpl certificateUpdate2 = PoolCertificateProjectionImpl.builder()
        .txId(2L).txEpochNo(6).certEpochNo(8).certIndex(0).poolUpdateId(2L)
        .blockTime(Timestamp.valueOf("2021-02-01 00:00:00")).build();
    PoolCertificateProjectionImpl certificateRetire1 = PoolCertificateProjectionImpl.builder()
        .txId(3L).txEpochNo(7).certEpochNo(9).certIndex(0).poolRetireId(1L)
        .blockTime(Timestamp.valueOf("2021-03-01 00:00:00")).build();
    PoolCertificateProjectionImpl certificateUpdate3 = PoolCertificateProjectionImpl.builder()
        .txId(4L).txEpochNo(11).certEpochNo(14).certIndex(0).poolUpdateId(3L)
        .blockTime(Timestamp.valueOf("2021-04-01 00:00:00")).build();

    String poolViewOrHash = "pool1akt37u42nfegyf3atv6c3c64da7dwa9grt9kjxa8zwj9qr4y8u2";
    when(poolUpdateRepository.getPoolUpdateByPoolViewOrHash(poolViewOrHash))
        .thenReturn(List.of(certificateUpdate1, certificateUpdate2, certificateUpdate3));
    when(poolRetireRepository.getPoolRetireByPoolViewOrHash(poolViewOrHash))
        .thenReturn(List.of(certificateRetire1));

    var response = poolCertificateService.getPoolCertificateByAction(poolViewOrHash,
                                                                     PoolActionType.POOL_REGISTRATION);

    Assertions.assertEquals(2, response.size());
    Assertions.assertEquals(PoolActionType.POOL_REGISTRATION, response.get(0).getActionType());
    Assertions.assertEquals(PoolActionType.POOL_REGISTRATION, response.get(1).getActionType());
    Assertions.assertEquals(1L, response.get(0).getTxId());
    Assertions.assertEquals(4L, response.get(1).getTxId());
  }

  @Test
  void getPoolCertificateByAction_shouldReturnCertificateUpdateList() {
    PoolCertificateProjectionImpl certificateUpdate1 = PoolCertificateProjectionImpl.builder()
        .txId(1L).txEpochNo(5).certEpochNo(8).certIndex(0).poolUpdateId(1L)
        .blockTime(Timestamp.valueOf("2021-01-01 00:00:00")).build();

    PoolCertificateProjectionImpl certificateUpdate2 = PoolCertificateProjectionImpl.builder()
        .txId(2L).txEpochNo(6).certEpochNo(8).certIndex(0).poolUpdateId(2L)
        .blockTime(Timestamp.valueOf("2021-02-01 00:00:00")).build();
    PoolCertificateProjectionImpl certificateRetire1 = PoolCertificateProjectionImpl.builder()
        .txId(3L).txEpochNo(7).certEpochNo(9).certIndex(0).poolRetireId(1L)
        .blockTime(Timestamp.valueOf("2021-03-01 00:00:00")).build();
    PoolCertificateProjectionImpl certificateUpdate3 = PoolCertificateProjectionImpl.builder()
        .txId(4L).txEpochNo(11).certEpochNo(14).certIndex(0).poolUpdateId(3L)
        .blockTime(Timestamp.valueOf("2021-04-01 00:00:00")).build();

    String poolViewOrHash = "pool1akt37u42nfegyf3atv6c3c64da7dwa9grt9kjxa8zwj9qr4y8u2";
    when(poolUpdateRepository.getPoolUpdateByPoolViewOrHash(poolViewOrHash))
        .thenReturn(List.of(certificateUpdate1, certificateUpdate2, certificateUpdate3));
    when(poolRetireRepository.getPoolRetireByPoolViewOrHash(poolViewOrHash))
        .thenReturn(List.of(certificateRetire1));

    var response = poolCertificateService.getPoolCertificateByAction(poolViewOrHash,
                                                                     PoolActionType.POOL_UPDATE);

    Assertions.assertEquals(1, response.size());
    Assertions.assertEquals(PoolActionType.POOL_UPDATE, response.get(0).getActionType());
    Assertions.assertEquals(2L, response.get(0).getTxId());
  }

  @Test
  void getPoolCertificateByAction_shouldReturnCertificateDeRegistrationList() {
    PoolCertificateProjectionImpl certificateUpdate1 = PoolCertificateProjectionImpl.builder()
        .txId(1L).txEpochNo(5).certEpochNo(8).certIndex(0).poolUpdateId(1L)
        .blockTime(Timestamp.valueOf("2021-01-01 00:00:00")).build();

    PoolCertificateProjectionImpl certificateUpdate2 = PoolCertificateProjectionImpl.builder()
        .txId(2L).txEpochNo(6).certEpochNo(8).certIndex(0).poolUpdateId(2L)
        .blockTime(Timestamp.valueOf("2021-02-01 00:00:00")).build();
    PoolCertificateProjectionImpl certificateRetire1 = PoolCertificateProjectionImpl.builder()
        .txId(3L).txEpochNo(7).certEpochNo(9).certIndex(0).poolRetireId(1L)
        .blockTime(Timestamp.valueOf("2021-03-01 00:00:00")).build();
    PoolCertificateProjectionImpl certificateUpdate3 = PoolCertificateProjectionImpl.builder()
        .txId(4L).txEpochNo(11).certEpochNo(14).certIndex(0).poolUpdateId(3L)
        .blockTime(Timestamp.valueOf("2021-04-01 00:00:00")).build();

    String poolViewOrHash = "pool1akt37u42nfegyf3atv6c3c64da7dwa9grt9kjxa8zwj9qr4y8u2";
    when(poolUpdateRepository.getPoolUpdateByPoolViewOrHash(poolViewOrHash))
        .thenReturn(List.of(certificateUpdate1, certificateUpdate2, certificateUpdate3));
    when(poolRetireRepository.getPoolRetireByPoolViewOrHash(poolViewOrHash))
        .thenReturn(List.of(certificateRetire1));

    var response = poolCertificateService.getPoolCertificateByAction(poolViewOrHash,
                                                                     PoolActionType.POOL_DEREGISTRATION);

    Assertions.assertEquals(1, response.size());
    Assertions.assertEquals(PoolActionType.POOL_DEREGISTRATION, response.get(0).getActionType());
    Assertions.assertEquals(3L, response.get(0).getTxId());
  }
}