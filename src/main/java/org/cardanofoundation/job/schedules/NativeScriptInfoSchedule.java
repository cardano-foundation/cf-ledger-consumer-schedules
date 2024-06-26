package org.cardanofoundation.job.schedules;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bloxbean.cardano.client.transaction.spec.script.NativeScript;
import com.bloxbean.cardano.client.transaction.spec.script.RequireTimeAfter;
import com.bloxbean.cardano.client.transaction.spec.script.RequireTimeBefore;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptAll;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptAny;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptAtLeast;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;

import org.cardanofoundation.explorer.common.entity.enumeration.ScriptType;
import org.cardanofoundation.explorer.common.entity.explorer.NativeScriptInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.BaseEntity_;
import org.cardanofoundation.explorer.common.entity.ledgersync.Script;
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.projection.ScriptNumberHolderProjection;
import org.cardanofoundation.job.projection.ScriptNumberTokenProjection;
import org.cardanofoundation.job.repository.explorer.NativeScriptInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.LatestTokenBalanceRepository;
import org.cardanofoundation.job.repository.ledgersync.MultiAssetRepository;
import org.cardanofoundation.job.repository.ledgersync.ScriptRepository;
import org.cardanofoundation.job.repository.ledgersync.TxRepository;

@Component
@RequiredArgsConstructor
@Log4j2
public class NativeScriptInfoSchedule {
  private final NativeScriptInfoRepository nativeScriptInfoRepository;
  private final ScriptRepository scriptRepository;
  private final MultiAssetRepository multiAssetRepository;
  private final LatestTokenBalanceRepository latestTokenBalanceRepository;
  private final TxRepository txRepository;

  private final RedisTemplate<String, Integer> redisTemplate;

  @Value("${application.network}")
  private String network;

  private static final int DEFAULT_PAGE_SIZE = 500;
  private static final List<ScriptType> NATIVE_SCRIPT_TYPES =
      List.of(ScriptType.TIMELOCK, ScriptType.MULTISIG);

  @PostConstruct
  void setup() {
    String nativeScriptTxCheckPoint = getRedisKey(RedisKey.NATIVE_SCRIPT_CHECKPOINT.name());
    log.info("Start setup for NativeScriptInfo jobs");
    redisTemplate.delete(nativeScriptTxCheckPoint);
  }

  @Scheduled(fixedDelayString = "${jobs.native-script-info.fixed-delay}")
  @Transactional(value = "explorerTransactionManager")
  public void syncNativeScriptInfo() {
    final String nativeScriptTxCheckPoint = getRedisKey(RedisKey.NATIVE_SCRIPT_CHECKPOINT.name());
    final Long currentTxId = txRepository.findCurrentTxInfo().getTxId();
    final Integer checkpoint = redisTemplate.opsForValue().get(nativeScriptTxCheckPoint);
    if (Objects.isNull(checkpoint) || nativeScriptInfoRepository.count() == 0L) {
      init();
    } else if (currentTxId > checkpoint.longValue()) {
      update(checkpoint, currentTxId);
    }
    redisTemplate
        .opsForValue()
        .set(nativeScriptTxCheckPoint, Math.max(currentTxId.intValue() - 1000, 0));
  }

  private void update(Integer checkpoint, Long currentTxId) {
    log.info("Start update NativeScriptInfo");
    long startTime = System.currentTimeMillis();
    Long txCheckpoint = checkpoint.longValue();
    Set<String> scriptHashList =
        multiAssetRepository.findPolicyByTxIn(txCheckpoint, currentTxId, NATIVE_SCRIPT_TYPES);

    final AtomicInteger counter = new AtomicInteger();
    scriptHashList.stream()
        .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / DEFAULT_PAGE_SIZE))
        .forEach(
            (page, scriptHashes) -> {
              List<Script> scripts = scriptRepository.findAllByHashIn(scriptHashes);
              saveNativeScriptInfo(scripts);
            });
    log.info(
        "End update NativeScriptInfo with size = {} in {} ms",
        scriptHashList.size(),
        System.currentTimeMillis() - startTime);
  }

  private void init() {
    log.info("Start init NativeScriptInfo");
    long startTime = System.currentTimeMillis();
    Pageable pageable =
        PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, BaseEntity_.ID));
    Slice<Script> scripts = scriptRepository.findAllByTypeIn(NATIVE_SCRIPT_TYPES, pageable);
    saveNativeScriptInfo(scripts.getContent());
    while (scripts.hasNext()) {
      scripts = scriptRepository.findAllByTypeIn(NATIVE_SCRIPT_TYPES, scripts.nextPageable());
      saveNativeScriptInfo(scripts.getContent());
    }
    log.info("End init NativeScriptInfo in {} ms", System.currentTimeMillis() - startTime);
  }

  private void saveNativeScriptInfo(List<Script> scripts) {
    Set<String> scriptHashes = scripts.stream().map(Script::getHash).collect(Collectors.toSet());
    Map<String, NativeScriptInfo> nativeScriptInfoMap =
        nativeScriptInfoRepository.findAllByScriptHashIn(scriptHashes).stream()
            .collect(Collectors.toMap(NativeScriptInfo::getScriptHash, Function.identity()));
    Map<String, Long> scriptNumberTokenMap =
        multiAssetRepository.countByPolicyIn(scriptHashes).stream()
            .collect(
                Collectors.toMap(
                    ScriptNumberTokenProjection::getScriptHash,
                    ScriptNumberTokenProjection::getNumberOfTokens));
    Map<String, Long> scriptNumberHolderMap =
        latestTokenBalanceRepository.countHolderByPolicyIn(scriptHashes).stream()
            .collect(
                Collectors.toMap(
                    ScriptNumberHolderProjection::getScriptHash,
                    ScriptNumberHolderProjection::getNumberOfHolders));
    List<NativeScriptInfo> nativeScriptInfoList =
        scripts.stream()
            .map(
                item -> {
                  NativeScriptInfo nativeScriptInfo;
                  if (Objects.nonNull(nativeScriptInfoMap.get(item.getHash()))) {
                    nativeScriptInfo = nativeScriptInfoMap.get(item.getHash());
                  } else {
                    nativeScriptInfo = createNativeScriptInfo(item);
                  }
                  nativeScriptInfo.setNumberOfTokens(
                      scriptNumberTokenMap.getOrDefault(item.getHash(), 0L));
                  nativeScriptInfo.setNumberOfAssetHolders(
                      scriptNumberHolderMap.getOrDefault(item.getHash(), 0L));
                  return nativeScriptInfo;
                })
            .toList();
    nativeScriptInfoRepository.saveAll(nativeScriptInfoList);
  }

  /**
   * create native script info from script
   *
   * @param script script entity
   * @return native script info
   */
  private NativeScriptInfo createNativeScriptInfo(Script script) {
    NativeScriptInfo nativeScriptInfo = new NativeScriptInfo();
    nativeScriptInfo.setScriptHash(script.getHash());
    nativeScriptInfo.setType(script.getType());
    try {
      NativeScript nativeScript = NativeScript.deserializeJson(script.getJson());
      explainNativeScript(nativeScript, nativeScriptInfo);
    } catch (Exception e) {
      log.debug("Error deserialize native script {}", script.getHash(), e);
    }
    return nativeScriptInfo;
  }

  /**
   * Explain native script
   *
   * @param nativeScript native script
   */
  public void explainNativeScript(NativeScript nativeScript, NativeScriptInfo nativeScriptInfo) {
    if (nativeScript.getClass().equals(ScriptPubkey.class)) {
      ScriptPubkey scriptPubkey = (ScriptPubkey) nativeScript;
      if (Objects.nonNull(scriptPubkey.getKeyHash())) {
        long numberSig =
            nativeScriptInfo.getNumberSig() == null ? 0L : nativeScriptInfo.getNumberSig();
        nativeScriptInfo.setNumberSig(numberSig + 1);
      }
    } else if (nativeScript.getClass().equals(ScriptAll.class)) {
      ScriptAll scriptAll = (ScriptAll) nativeScript;
      for (NativeScript script : scriptAll.getScripts()) {
        explainNativeScript(script, nativeScriptInfo);
      }
    } else if (nativeScript.getClass().equals(ScriptAny.class)) {
      ScriptAny scriptAny = (ScriptAny) nativeScript;
      for (NativeScript script : scriptAny.getScripts()) {
        explainNativeScript(script, nativeScriptInfo);
      }
    } else if (nativeScript.getClass().equals(ScriptAtLeast.class)) {
      ScriptAtLeast scriptAtLeast = (ScriptAtLeast) nativeScript;
      for (NativeScript script : scriptAtLeast.getScripts()) {
        explainNativeScript(script, nativeScriptInfo);
      }
    } else if (nativeScript.getClass().equals(RequireTimeAfter.class)) {
      RequireTimeAfter requireTimeAfter = (RequireTimeAfter) nativeScript;
      nativeScriptInfo.setAfterSlot(requireTimeAfter.getSlot());
    } else if (nativeScript.getClass().equals(RequireTimeBefore.class)) {
      RequireTimeBefore requireTimeBefore = (RequireTimeBefore) nativeScript;
      nativeScriptInfo.setBeforeSlot(requireTimeBefore.getSlot());
    }
  }

  private String getRedisKey(String prefix) {
    return prefix + "_" + network;
  }
}
