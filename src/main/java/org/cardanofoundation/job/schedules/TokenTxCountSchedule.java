package org.cardanofoundation.job.schedules;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import jakarta.annotation.PostConstruct;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.common.entity.ledgersync.BaseEntity_;
import org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount;
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.repository.ledgersync.MultiAssetRepository;
import org.cardanofoundation.job.repository.ledgersync.jooq.JOOQTokenTxCountRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;
import org.cardanofoundation.job.service.TokenInfoServiceAsync;
import org.cardanofoundation.job.util.BatchUtils;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class TokenTxCountSchedule {

  private final AddressTxAmountRepository addressTxAmountRepository;

  @Value("${application.network}")
  private String network;

  private final MultiAssetRepository multiAssetRepository;
  private final JOOQTokenTxCountRepository jooqTokenTxCountRepository;
  private final TokenInfoServiceAsync tokenInfoServiceAsync;
  private final RedisTemplate<String, Integer> redisTemplate;

  private static final int DEFAULT_PAGE_SIZE = 10000;

  @PostConstruct
  void setup() {
    String nativeScriptTxCheckPoint = getRedisKey(RedisKey.TOKEN_TX_COUNT_CHECKPOINT.name());
    redisTemplate.delete(nativeScriptTxCheckPoint);
  }

  private String getRedisKey(String prefix) {
    return prefix + "_" + network;
  }

  @Scheduled(fixedDelayString = "${jobs.token-info.fixed-delay}")
  @Transactional
  public void syncTokenTxCount() {
    final String tokenTxCountCheckPoint = getRedisKey(RedisKey.TOKEN_TX_COUNT_CHECKPOINT.name());
    final long currentEpochSecond = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
    final Integer checkpoint = redisTemplate.opsForValue().get(tokenTxCountCheckPoint);
    if (Objects.isNull(checkpoint)) {
      init();
    } else if (currentEpochSecond > checkpoint.longValue()) {
      update(checkpoint.longValue(), currentEpochSecond);
    }

    // Update the checkpoint to the current epoch second minus 5 hours.
    redisTemplate
        .opsForValue()
        .set(tokenTxCountCheckPoint, Math.max((int) currentEpochSecond - 18000, 0));
  }

  private void init() {
    log.info("Start init TokenTxCount");
    long startTime = System.currentTimeMillis();

    List<CompletableFuture<List<TokenTxCount>>> tokenTxCountNeedSaveFutures = new ArrayList<>();

    Pageable pageable =
        PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, BaseEntity_.ID));
    Slice<String> multiAssetSlice = multiAssetRepository.getTokenUnitSlice(pageable);

    tokenTxCountNeedSaveFutures.add(
        tokenInfoServiceAsync.buildTokenTxCountList(multiAssetSlice.getContent()));
    while (multiAssetSlice.hasNext()) {
      multiAssetSlice = multiAssetRepository.getTokenUnitSlice(multiAssetSlice.nextPageable());
      tokenTxCountNeedSaveFutures.add(
          tokenInfoServiceAsync.buildTokenTxCountList(multiAssetSlice.getContent()));

      // After every 5 batches, insert the fetched token tx count data into the database in batches.
      if (tokenTxCountNeedSaveFutures.size() % 5 == 0) {
        var tokenTxCountList =
            tokenTxCountNeedSaveFutures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        BatchUtils.doBatching(1000, tokenTxCountList, jooqTokenTxCountRepository::insertAll);
        tokenTxCountNeedSaveFutures.clear();
      }
    }

    // Insert the remaining token tx count data into the database.
    var tokenTxCountList =
        tokenTxCountNeedSaveFutures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .toList();
    BatchUtils.doBatching(1000, tokenTxCountList, jooqTokenTxCountRepository::insertAll);

    log.info("End init TokenTxCount in {} ms", System.currentTimeMillis() - startTime);
  }

  private void update(Long epochSecondCheckpoint, Long currentEpochSecond) {
    log.info("Start update TokenTxCount");
    long startTime = System.currentTimeMillis();
    List<String> units =
        addressTxAmountRepository.findUnitByBlockTimeInRange(
            epochSecondCheckpoint, currentEpochSecond);

    List<TokenTxCount> tokenTxCounts = tokenInfoServiceAsync.buildTokenTxCountList(units).join();
    BatchUtils.doBatching(1000, tokenTxCounts, jooqTokenTxCountRepository::insertAll);
    log.info(
        "End update TokenTxCount with size = {} in {} ms",
        units.size(),
        System.currentTimeMillis() - startTime);
  }
}
