package org.cardanofoundation.job.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.explorer.common.entity.enumeration.ScriptType;
import org.cardanofoundation.explorer.common.entity.explorer.NativeScriptInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.Script;
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.projection.ScriptNumberHolderProjection;
import org.cardanofoundation.job.projection.ScriptNumberTokenProjection;
import org.cardanofoundation.job.repository.explorer.NativeScriptInfoRepository;
import org.cardanofoundation.job.repository.explorer.jooq.JOOQNativeScriptInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.ScriptRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.schedules.NativeScriptInfoSchedule;
import org.cardanofoundation.job.service.NativeScriptInfoServiceAsync;

@ExtendWith(MockitoExtension.class)
public class NativeScriptInfoScheduleTest {

  @Mock RedisTemplate<String, Integer> redisTemplate;
  @Mock ValueOperations valueOperations;
  @Mock NativeScriptInfoRepository nativeScriptInfoRepository;
  @Mock ScriptRepository scriptRepository;
  @Mock AddressTxAmountRepository addressTxAmountRepository;
  @Mock JOOQNativeScriptInfoRepository jooqNativeScriptInfoRepository;
  @Mock NativeScriptInfoServiceAsync nativeScriptInfoServiceAsync;

  @Captor ArgumentCaptor<List<NativeScriptInfo>> argumentCaptorNativeScriptInfo;

  NativeScriptInfoSchedule nativeScriptInfoSchedule;

  @BeforeEach
  void setUp() {
    nativeScriptInfoSchedule =
        new NativeScriptInfoSchedule(
            nativeScriptInfoRepository,
            scriptRepository,
            addressTxAmountRepository,
            jooqNativeScriptInfoRepository,
            nativeScriptInfoServiceAsync,
            redisTemplate);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    ReflectionTestUtils.setField(nativeScriptInfoSchedule, "network", "mainnet");
  }

  @Test
  void syncNativeScriptInfo_shouldInitDataWhenSyncDataFirstTime() {
    when(redisTemplate.opsForValue().get(getRedisKey(RedisKey.NATIVE_SCRIPT_CHECKPOINT.name())))
        .thenReturn(null);
    String scriptHash = "3a9241cd79895e3a8d65261b40077d4437ce71e9d7c8c6c00e3f658e";
    Script script =
        Script.builder()
            .id(1L)
            .type(ScriptType.TIMELOCK)
            .hash(scriptHash)
            .json(
                "{\"type\":\"all\",\"scripts\":[{\"type\":\"sig\",\"keyHash\":\"26bacc7b88e2b40701387c521cd0c50d5c0cfa4c6c6d7f0901395757\"},{\"type\":\"before\",\"slot\":23069343}]}")
            .build();

    when(scriptRepository.findAllByTypeIn(anyList(), any(Pageable.class)))
        .thenReturn(new SliceImpl<>(List.of(script)));
    when(nativeScriptInfoRepository.findAllByScriptHashIn(Set.of(scriptHash)))
        .thenReturn(Collections.emptyList());

    ScriptNumberTokenProjection scriptNumberTokenProjection =
        Mockito.mock(ScriptNumberTokenProjection.class);
    when(scriptNumberTokenProjection.getScriptHash()).thenReturn(scriptHash);
    when(scriptNumberTokenProjection.getNumberOfTokens()).thenReturn(1L);
    //    when(multiAssetRepository.countByPolicyIn(Set.of(scriptHash)))
    //        .thenReturn(List.of(scriptNumberTokenProjection));

    ScriptNumberHolderProjection scriptNumberHolderProjection =
        Mockito.mock(ScriptNumberHolderProjection.class);
    when(scriptNumberHolderProjection.getScriptHash()).thenReturn(scriptHash);
    when(scriptNumberHolderProjection.getNumberOfHolders()).thenReturn(1L);
    //    when(latestTokenBalanceRepository.countHolderByPolicyIn(Set.of(scriptHash)))
    //        .thenReturn(List.of(scriptNumberHolderProjection));

    nativeScriptInfoSchedule.syncNativeScriptInfo();
    verify(nativeScriptInfoRepository, Mockito.times(1))
        .saveAll(argumentCaptorNativeScriptInfo.capture());

    List<NativeScriptInfo> nativeScriptInfoList = argumentCaptorNativeScriptInfo.getValue();
    Assertions.assertEquals(1, nativeScriptInfoList.size());
    Assertions.assertEquals(script.getHash(), nativeScriptInfoList.get(0).getScriptHash());
    Assertions.assertEquals(script.getType(), nativeScriptInfoList.get(0).getType());
    Assertions.assertEquals(1, nativeScriptInfoList.get(0).getNumberOfAssetHolders());
    Assertions.assertEquals(1, nativeScriptInfoList.get(0).getNumberOfTokens());
    Assertions.assertEquals(23069343, nativeScriptInfoList.get(0).getBeforeSlot());
    Assertions.assertEquals(1, nativeScriptInfoList.get(0).getNumberSig());
  }

  @Test
  void syncSmartContractInfo_shouldUpdateDataWhenSyncDataNotFirstTime() {
    when(redisTemplate.opsForValue().get(getRedisKey(RedisKey.NATIVE_SCRIPT_CHECKPOINT.name())))
        .thenReturn(3);
    when(nativeScriptInfoRepository.count()).thenReturn(1L);
    String scriptHash = "02f68378e37af4545d027d0a9fa5581ac682897a3fc1f6d8f936ed2b";
    Script script =
        Script.builder()
            .id(1L)
            .type(ScriptType.TIMELOCK)
            .hash(scriptHash)
            .json(
                "{\"type\":\"sig\",\"keyHash\":\"89d555c7a028bc560ea44e52a81c866088566f3a99c9989a5a183940\"}")
            .build();

    when(addressTxAmountRepository.findPolicyByBlockTimeInRange(any(), any()))
        .thenReturn(List.of(scriptHash));
    when(scriptRepository.findAllByHashInAndTypeIn(anyList(), anyList()))
        .thenReturn(List.of(script));
    when(nativeScriptInfoRepository.findAllByScriptHashIn(Set.of(scriptHash)))
        .thenReturn(Collections.emptyList());

    ScriptNumberTokenProjection scriptNumberTokenProjection =
        Mockito.mock(ScriptNumberTokenProjection.class);
    when(scriptNumberTokenProjection.getScriptHash()).thenReturn(scriptHash);
    when(scriptNumberTokenProjection.getNumberOfTokens()).thenReturn(3L);
    //    when(multiAssetRepository.countByPolicyIn(Set.of(scriptHash)))
    //        .thenReturn(List.of(scriptNumberTokenProjection));

    ScriptNumberHolderProjection scriptNumberHolderProjection =
        Mockito.mock(ScriptNumberHolderProjection.class);
    when(scriptNumberHolderProjection.getScriptHash()).thenReturn(scriptHash);
    when(scriptNumberHolderProjection.getNumberOfHolders()).thenReturn(15L);
    //    when(latestTokenBalanceRepository.countHolderByPolicyIn(Set.of(scriptHash)))
    //        .thenReturn(List.of(scriptNumberHolderProjection));

    nativeScriptInfoSchedule.syncNativeScriptInfo();
    verify(nativeScriptInfoRepository, Mockito.times(1))
        .saveAll(argumentCaptorNativeScriptInfo.capture());

    List<NativeScriptInfo> nativeScriptInfoList = argumentCaptorNativeScriptInfo.getValue();
    Assertions.assertEquals(1, nativeScriptInfoList.size());
    Assertions.assertEquals(script.getHash(), nativeScriptInfoList.get(0).getScriptHash());
    Assertions.assertEquals(script.getType(), nativeScriptInfoList.get(0).getType());
    Assertions.assertEquals(15, nativeScriptInfoList.get(0).getNumberOfAssetHolders());
    Assertions.assertEquals(3, nativeScriptInfoList.get(0).getNumberOfTokens());
    Assertions.assertEquals(1, nativeScriptInfoList.get(0).getNumberSig());
  }

  private String getRedisKey(String prefix) {
    return prefix + "_" + "mainnet";
  }
}
