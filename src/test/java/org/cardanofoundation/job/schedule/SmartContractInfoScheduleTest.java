package org.cardanofoundation.job.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.CollectionUtils;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.explorer.common.entity.enumeration.ScriptPurposeType;
import org.cardanofoundation.explorer.common.entity.enumeration.ScriptType;
import org.cardanofoundation.explorer.common.entity.explorer.SmartContractInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.Script;
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.projection.SContractPurposeProjection;
import org.cardanofoundation.job.projection.SContractTxCntProjection;
import org.cardanofoundation.job.projection.TxInfoProjection;
import org.cardanofoundation.job.provider.RedisProvider;
import org.cardanofoundation.job.repository.explorer.SmartContractInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.RedeemerRepository;
import org.cardanofoundation.job.repository.ledgersync.ScriptRepository;
import org.cardanofoundation.job.repository.ledgersync.TxRepository;
import org.cardanofoundation.job.schedules.SmartContractInfoSchedule;

@ExtendWith(MockitoExtension.class)
public class SmartContractInfoScheduleTest {

  @Mock RedisProvider<String, Integer> redisProvider;
  @Mock SmartContractInfoRepository smartContractInfoRepository;
  @Mock ScriptRepository scriptRepository;
  @Mock RedeemerRepository redeemerRepository;
  @Mock TxRepository txRepository;

  @Captor ArgumentCaptor<List<SmartContractInfo>> argumentCaptorSmartContractInfo;

  SmartContractInfoSchedule smartContractInfoSchedule;

  @BeforeEach
  void setUp() {
    Set<String> keys = redisProvider.keys("*");
    if (!CollectionUtils.isEmpty(keys)) {
      redisProvider.del(keys);
    }
    smartContractInfoSchedule =
        new SmartContractInfoSchedule(
            redisProvider,
            smartContractInfoRepository,
            scriptRepository,
            redeemerRepository,
            txRepository);
    ReflectionTestUtils.setField(smartContractInfoSchedule, "network", "mainnet");
  }

  @Test
  void syncSmartContractInfo_shouldInitDataWhenSyncDataFirstTime() {
    TxInfoProjection txInfoProjection = Mockito.mock(TxInfoProjection.class);
    when(txInfoProjection.getTxId()).thenReturn(10L);
    when(txRepository.findCurrentTxInfo()).thenReturn(txInfoProjection);

    Script script =
        Script.builder()
            .id(1L)
            .type(ScriptType.PLUTUSV1)
            .hash("e4d2fb0b8d275852103fd75801e2c7dcf6ed3e276c74cabadbe5b8b6")
            .build();

    when(scriptRepository.findAllByTypeIn(anyList(), any(Pageable.class)))
        .thenReturn(new SliceImpl<>(List.of(script)));

    SContractTxCntProjection sContractTxCntProjection =
        Mockito.mock(SContractTxCntProjection.class);
    when(sContractTxCntProjection.getTxCount()).thenReturn(1L);
    when(sContractTxCntProjection.getScriptHash()).thenReturn(script.getHash());
    when(redeemerRepository.getSContractTxCountByHashIn(List.of(script.getHash())))
        .thenReturn(List.of(sContractTxCntProjection));

    SContractPurposeProjection sContractPurposeProjection =
        Mockito.mock(SContractPurposeProjection.class);
    when(sContractPurposeProjection.getScriptHash()).thenReturn(script.getHash());
    when(sContractPurposeProjection.getScriptPurposeType()).thenReturn(ScriptPurposeType.CERT);
    when(redeemerRepository.getScriptPurposeTypeByScriptHashIn(List.of(script.getHash())))
        .thenReturn(List.of(sContractPurposeProjection));

    when(smartContractInfoRepository.findAllByScriptHashIn(List.of(script.getHash())))
        .thenReturn(List.of());

    smartContractInfoSchedule.syncSmartContractInfo();
    verify(smartContractInfoRepository, Mockito.times(1))
        .saveAll(argumentCaptorSmartContractInfo.capture());

    List<SmartContractInfo> smartContractInfoList = argumentCaptorSmartContractInfo.getValue();
    Assertions.assertEquals(1, smartContractInfoList.size());
    Assertions.assertEquals(script.getHash(), smartContractInfoList.get(0).getScriptHash());
    Assertions.assertEquals(script.getType(), smartContractInfoList.get(0).getType());
    Assertions.assertEquals(1, smartContractInfoList.get(0).getTxCount());
    Assertions.assertEquals(true, smartContractInfoList.get(0).getIsScriptCert());
  }

  @Test
  void syncSmartContractInfo_shouldUpdateDataWhenSyncDataNotFirstTime() {
    TxInfoProjection txInfoProjection = Mockito.mock(TxInfoProjection.class);
    when(txInfoProjection.getTxId()).thenReturn(10L);
    when(txRepository.findCurrentTxInfo()).thenReturn(txInfoProjection);
    when(smartContractInfoRepository.count()).thenReturn(1L);

    when(redisProvider.getValueByKey(redisProvider.getRedisKey(RedisKey.SC_TX_CHECKPOINT.name())))
        .thenReturn(3);
    Script script =
        Script.builder()
            .id(1L)
            .type(ScriptType.PLUTUSV1)
            .hash("e4d2fb0b8d275852103fd75801e2c7dcf6ed3e276c74cabadbe5b8b6")
            .build();

    when(scriptRepository.findAllByTxIn(any(Long.class), any(Long.class), any(Pageable.class)))
        .thenReturn(new SliceImpl<>(List.of(script)));

    SContractTxCntProjection sContractTxCntProjection =
        Mockito.mock(SContractTxCntProjection.class);
    when(sContractTxCntProjection.getTxCount()).thenReturn(1L);
    when(sContractTxCntProjection.getScriptHash()).thenReturn(script.getHash());
    when(redeemerRepository.getSContractTxCountByHashIn(List.of(script.getHash())))
        .thenReturn(List.of(sContractTxCntProjection));

    SContractPurposeProjection sContractPurposeProjection =
        Mockito.mock(SContractPurposeProjection.class);
    when(sContractPurposeProjection.getScriptHash()).thenReturn(script.getHash());
    when(sContractPurposeProjection.getScriptPurposeType()).thenReturn(ScriptPurposeType.CERT);
    when(redeemerRepository.getScriptPurposeTypeByScriptHashIn(List.of(script.getHash())))
        .thenReturn(List.of(sContractPurposeProjection));

    when(smartContractInfoRepository.findAllByScriptHashIn(List.of(script.getHash())))
        .thenReturn(List.of());

    smartContractInfoSchedule.syncSmartContractInfo();
    verify(smartContractInfoRepository, Mockito.times(1))
        .saveAll(argumentCaptorSmartContractInfo.capture());

    List<SmartContractInfo> smartContractInfoList = argumentCaptorSmartContractInfo.getValue();
    Assertions.assertEquals(1, smartContractInfoList.size());
    Assertions.assertEquals(script.getHash(), smartContractInfoList.get(0).getScriptHash());
    Assertions.assertEquals(script.getType(), smartContractInfoList.get(0).getType());
    Assertions.assertEquals(1, smartContractInfoList.get(0).getTxCount());
    Assertions.assertEquals(true, smartContractInfoList.get(0).getIsScriptCert());
  }
}
