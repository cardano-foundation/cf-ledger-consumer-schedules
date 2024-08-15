package org.cardanofoundation.job.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bloxbean.cardano.client.transaction.spec.script.NativeScript;
import com.bloxbean.cardano.client.transaction.spec.script.RequireTimeAfter;
import com.bloxbean.cardano.client.transaction.spec.script.RequireTimeBefore;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptAll;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptAny;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptAtLeast;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;

import org.cardanofoundation.explorer.common.entity.explorer.NativeScriptInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.Script;
import org.cardanofoundation.job.projection.ScriptNumberHolderProjection;
import org.cardanofoundation.job.projection.ScriptNumberTokenProjection;
import org.cardanofoundation.job.repository.explorer.NativeScriptInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.MultiAssetRepository;
import org.cardanofoundation.job.repository.ledgersync.ScriptRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.LatestTokenBalanceRepository;

@Component
@RequiredArgsConstructor
@Log4j2
public class NativeScriptInfoServiceAsync {

  private final NativeScriptInfoRepository nativeScriptInfoRepository;
  private final ScriptRepository scriptRepository;
  private final MultiAssetRepository multiAssetRepository;
  private final LatestTokenBalanceRepository latestTokenBalanceRepository;
  private final AddressTxAmountRepository addressTxAmountRepository;

  @Async
  @Transactional(readOnly = true)
  public CompletableFuture<List<NativeScriptInfo>> buildNativeScriptInfoList(List<Script> scripts) {
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

    return CompletableFuture.completedFuture(nativeScriptInfoList);
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
}
