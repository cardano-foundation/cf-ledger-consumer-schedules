package org.cardanofoundation.job.schedules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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

import org.cardanofoundation.explorer.common.entity.enumeration.ScriptType;
import org.cardanofoundation.explorer.common.entity.explorer.NativeScriptInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.BaseEntity_;
import org.cardanofoundation.explorer.common.entity.ledgersync.Block;
import org.cardanofoundation.explorer.common.entity.ledgersync.Script;
import org.cardanofoundation.job.common.constant.Constant;
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.repository.explorer.NativeScriptInfoRepository;
import org.cardanofoundation.job.repository.explorer.jooq.JOOQNativeScriptInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.ScriptRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.service.NativeScriptInfoServiceAsync;
import org.cardanofoundation.job.util.BatchUtils;

@Component
@RequiredArgsConstructor
@Log4j2
public class NativeScriptInfoSchedule {
  private final NativeScriptInfoRepository nativeScriptInfoRepository;
  private final ScriptRepository scriptRepository;
  private final AddressTxAmountRepository addressTxAmountRepository;
  private final JOOQNativeScriptInfoRepository jooqNativeScriptInfoRepository;
  private final NativeScriptInfoServiceAsync nativeScriptInfoServiceAsync;
  private final BlockRepository blockRepository;
  private final RedisTemplate<String, Integer> redisTemplate;

  @Value("${application.network}")
  private String network;

  private static final int DEFAULT_PAGE_SIZE = 1000;
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
    final Integer checkpoint = redisTemplate.opsForValue().get(nativeScriptTxCheckPoint);

    final long currentLSAggSlot = addressTxAmountRepository.getMaxSlotNoFromCursor();
    final long currentLSSlot = blockRepository.findLatestBlock().map(Block::getSlotNo).orElse(0L);

    long currentSlot = Math.min(currentLSAggSlot, currentLSSlot);
    if (Objects.isNull(checkpoint) || nativeScriptInfoRepository.count() == 0L) {
      init();
    } else if (currentSlot > checkpoint.longValue()) {
      update(Long.valueOf(checkpoint), currentSlot);
    }
    redisTemplate
        .opsForValue()
        .set(nativeScriptTxCheckPoint, Math.max((int) currentSlot - Constant.ROLLBACKSLOT, 0));
  }

  private void update(Long epochSecondCheckpoint, Long currentEpochSecond) {
    log.info("Start update NativeScriptInfo");
    long startTime = System.currentTimeMillis();

    List<String> scriptHashList =
        addressTxAmountRepository.findPolicyBySlotInRange(
            epochSecondCheckpoint, currentEpochSecond);

    List<Script> scripts =
        scriptRepository.findAllByHashInAndTypeIn(scriptHashList, NATIVE_SCRIPT_TYPES);

    List<NativeScriptInfo> nativeScriptInfos =
        nativeScriptInfoServiceAsync.buildNativeScriptInfoList(scripts).join();

    jooqNativeScriptInfoRepository.insertAll(nativeScriptInfos);
    log.info(
        "End update NativeScriptInfo with size = {} in {} ms",
        scriptHashList.size(),
        System.currentTimeMillis() - startTime);
  }

  private void init() {
    log.info("Start init NativeScriptInfo");
    long startTime = System.currentTimeMillis();
    List<CompletableFuture<List<NativeScriptInfo>>> nativeScriptInfoNeedSaveFutures =
        new ArrayList<>();

    Pageable pageable =
        PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, BaseEntity_.ID));
    Slice<Script> scripts = scriptRepository.findAllByTypeIn(NATIVE_SCRIPT_TYPES, pageable);

    nativeScriptInfoNeedSaveFutures.add(
        nativeScriptInfoServiceAsync.buildNativeScriptInfoList(scripts.getContent()));

    while (scripts.hasNext()) {
      scripts = scriptRepository.findAllByTypeIn(NATIVE_SCRIPT_TYPES, scripts.nextPageable());
      nativeScriptInfoNeedSaveFutures.add(
          nativeScriptInfoServiceAsync.buildNativeScriptInfoList(scripts.getContent()));

      // After every 5 batches, insert the fetched native script info data into the database in
      // batches.
      if (nativeScriptInfoNeedSaveFutures.size() % 5 == 0) {
        var nativeScriptInfoList =
            nativeScriptInfoNeedSaveFutures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        BatchUtils.doBatching(
            1000, nativeScriptInfoList, jooqNativeScriptInfoRepository::insertAll);
        nativeScriptInfoNeedSaveFutures.clear();
      }
    }

    // Insert the remaining native script info data into the database.
    var nativeScriptInfoList =
        nativeScriptInfoNeedSaveFutures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .toList();

    BatchUtils.doBatching(1000, nativeScriptInfoList, jooqNativeScriptInfoRepository::insertAll);
    log.info("End init NativeScriptInfo in {} ms", System.currentTimeMillis() - startTime);
  }

  private String getRedisKey(String prefix) {
    return prefix + "_" + network;
  }
}
