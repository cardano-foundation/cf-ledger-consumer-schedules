package org.cardanofoundation.job.schedules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.consumercommon.entity.Script;
import org.cardanofoundation.explorer.consumercommon.enumeration.ScriptPurposeType;
import org.cardanofoundation.explorer.consumercommon.enumeration.ScriptType;
import org.cardanofoundation.explorer.consumercommon.explorer.entity.SmartContractInfo;
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.projection.SContractPurposeProjection;
import org.cardanofoundation.job.projection.SContractTxCntProjection;
import org.cardanofoundation.job.repository.explorer.SmartContractInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.RedeemerRepository;
import org.cardanofoundation.job.repository.ledgersync.ScriptRepository;
import org.cardanofoundation.job.repository.ledgersync.TxRepository;

@Component
@RequiredArgsConstructor
@Log4j2
public class SmartContractInfoSchedule {
  private static final int DEFAULT_PAGE_SIZE = 500;
  final RedisTemplate<String, Integer> redisTemplate;

  private final SmartContractInfoRepository smartContractInfoRepository;
  private final ScriptRepository scriptRepository;
  private final RedeemerRepository redeemerRepository;
  private final TxRepository txRepository;

  @Value("${application.network}")
  String network;

  @PostConstruct
  public void setup() {
    redisTemplate.delete(getRedisKey(RedisKey.SC_TX_CHECKPOINT.name()));
  }

  @Scheduled(fixedRateString = "${jobs.smart-contract-info.fixed-delay}")
  @Transactional(value = "explorerTransactionManager")
  public void syncSmartContractInfo() {
    log.info("Start job syncSmartContractInfo");
    long startTime = System.currentTimeMillis();

    final String scTxCheckpointKey = getRedisKey(RedisKey.SC_TX_CHECKPOINT.name());
    final Long currentTxId = txRepository.findCurrentTxInfo().getTxId();
    Slice<Script> scriptSlice;
    boolean flagInit = false;
    if (redisTemplate.opsForValue().get(scTxCheckpointKey) == null ||
        smartContractInfoRepository.count() == 0) {
      scriptSlice = scriptRepository.findAllByTypeIn(
          Arrays.asList(ScriptType.PLUTUSV1, ScriptType.PLUTUSV2),
          PageRequest.of(0, DEFAULT_PAGE_SIZE));
      flagInit = true;
    } else {
      Long scTxCheckpoint = Long.valueOf(redisTemplate.opsForValue().get(scTxCheckpointKey));
      scriptSlice = scriptRepository.findAllByTxIn(
          scTxCheckpoint, txRepository.findCurrentTxInfo().getTxId(),
          PageRequest.of(0, DEFAULT_PAGE_SIZE));
    }

    saveSmartContractInfo(scriptSlice.getContent());

    while (scriptSlice.hasNext()) {
      scriptSlice = flagInit ?
                    scriptRepository
                        .findAllByTypeIn(Arrays.asList(ScriptType.PLUTUSV1, ScriptType.PLUTUSV2),
                                         scriptSlice.nextPageable())
                             :
                    scriptRepository.findAllByTxIn(
                        scriptSlice.nextPageable().getOffset(),
                        txRepository.findCurrentTxInfo().getTxId(),
                        scriptSlice.nextPageable());

      saveSmartContractInfo(scriptSlice.getContent());
    }
    redisTemplate.opsForValue().set(scTxCheckpointKey, currentTxId.intValue());
    log.info("End Job syncSmartContractInfo, Time taken {}ms",
             System.currentTimeMillis() - startTime);
  }


  void saveSmartContractInfo(List<Script> scriptList) {
    if(scriptList.isEmpty()) {
      return;
    }

    long startTime = System.currentTimeMillis();
    log.info("Start saveSmartContractInfo, scriptList size {}", scriptList.size());
    List<String> scriptHashes = scriptList.stream().map(Script::getHash)
        .collect(Collectors.toList());

    Map<String, Long> sContractTxCntList = redeemerRepository
        .getSContractTxCountByHashIn(scriptHashes).stream()
        .collect(Collectors.toMap(SContractTxCntProjection::getScriptHash,
                                  SContractTxCntProjection::getTxCount));

    Map<String, List<SContractPurposeProjection>> sContractPurposeList = redeemerRepository
        .getScriptPurposeTypeByScriptHashIn(scriptHashes)
        .stream()
        .collect(Collectors.groupingBy(SContractPurposeProjection::getScriptHash));

    // get existing smart contract info
    Map<String, SmartContractInfo> smartContractInfoMap = smartContractInfoRepository
        .findAllByScriptHashIn(scriptHashes).stream()
        .collect(Collectors.toMap(SmartContractInfo::getScriptHash, Function.identity()));

    List<SmartContractInfo> smartContractInfoNeedSave = new ArrayList<>();

    // add all existing smart contract info to save list
    smartContractInfoNeedSave.addAll(smartContractInfoMap.values());

    // add all new smart contract info to save list
    smartContractInfoNeedSave.addAll(
        scriptList.stream().filter(script -> !smartContractInfoMap.containsKey(script.getHash()))
            .map(script -> SmartContractInfo.builder()
                .scriptHash(script.getHash())
                .type(script.getType())
                .build())
            .collect(Collectors.toList()));

    smartContractInfoNeedSave.forEach(smartContractInfo -> {
      smartContractInfo.setTxCount(
          sContractTxCntList.getOrDefault(smartContractInfo.getScriptHash(), 0L));
      Set<ScriptPurposeType> sContractPurposeProjectionList =
          sContractPurposeList.getOrDefault(smartContractInfo.getScriptHash(), new ArrayList<>())
              .stream()
              .map(SContractPurposeProjection::getScriptPurposeType)
              .collect(Collectors.toSet());

      if(sContractPurposeProjectionList.contains(ScriptPurposeType.MINT)) {
        smartContractInfo.setIsScriptMint(true);
      }
      if(sContractPurposeProjectionList.contains(ScriptPurposeType.SPEND)) {
        smartContractInfo.setIsScriptSpend(true);
      }
      if(sContractPurposeProjectionList.contains(ScriptPurposeType.CERT)) {
        smartContractInfo.setIsScriptCert(true);
      }
      if(sContractPurposeProjectionList.contains(ScriptPurposeType.REWARD)) {
        smartContractInfo.setIsScriptReward(true);
      }
    });

    smartContractInfoRepository.saveAll(smartContractInfoNeedSave);
    log.info("End saveSmartContractInfo, Time taken {} ms",
             System.currentTimeMillis() - startTime);
  }

  private String getRedisKey(String prefix) {
    return prefix + "_" + network;
  }
}
