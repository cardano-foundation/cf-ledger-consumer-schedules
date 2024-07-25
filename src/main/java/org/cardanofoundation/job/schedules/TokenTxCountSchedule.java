package org.cardanofoundation.job.schedules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
import org.cardanofoundation.explorer.common.entity.ledgersync.Block;
import org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount;
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.MultiAssetRepository;
import org.cardanofoundation.job.repository.ledgersync.TokenTxCountRepository;
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
  private final TokenTxCountRepository tokenTxCountRepository;
  private final MultiAssetRepository multiAssetRepository;
  private final JOOQTokenTxCountRepository jooqTokenTxCountRepository;
  private final TokenInfoServiceAsync tokenInfoServiceAsync;
  private final BlockRepository blockRepository;
  private final RedisTemplate<String, Integer> redisTemplate;

  @Value("${application.network}")
  private String network;

  private static final int DEFAULT_PAGE_SIZE = 100;

  private String getRedisKey(String prefix) {
    return prefix + "_" + network;
  }

  @Scheduled(fixedDelayString = "${jobs.token-info.fixed-delay}")
  @Transactional
  public void syncTokenTxCount() {
    final String tokenTxCountCheckPoint = getRedisKey(RedisKey.TOKEN_TX_COUNT_CHECKPOINT.name());

    Optional<Block> latestBlock = blockRepository.findLatestBlock();
    if (latestBlock.isEmpty()) {
      return;
    }

    final long currentMaxSlotNo =
        Math.min(addressTxAmountRepository.getMaxSlotNoFromCursor(), latestBlock.get().getSlotNo());
    final Integer checkpoint = redisTemplate.opsForValue().get(tokenTxCountCheckPoint);

    if (Objects.isNull(checkpoint) || tokenTxCountRepository.count() == 0) {
      init();
    } else if (currentMaxSlotNo > checkpoint.longValue()) {
      update(checkpoint.longValue(), currentMaxSlotNo);
    }

    // Update the checkpoint to the currentMaxSlotNo - 43200 (slot) to avoid missing any data when
    // node
    // rollback
    redisTemplate
        .opsForValue()
        .set(tokenTxCountCheckPoint, Math.max((int) currentMaxSlotNo - 43200, 0));
  }

  private void init() {
    log.info("Start init TokenTxCount");
    long startTime = System.currentTimeMillis();
    long index = 1;
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
      index++;
      // After every 5 batches, insert the fetched token tx count data into the database in batches.
      if (tokenTxCountNeedSaveFutures.size() % 10 == 0) {
        log.info("Inserting token tx count data into the database");
        var tokenTxCountList =
            tokenTxCountNeedSaveFutures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        BatchUtils.doBatching(1000, tokenTxCountList, jooqTokenTxCountRepository::insertAll);
        tokenTxCountNeedSaveFutures.clear();
        log.info("Total saved token tx count: {}", index * DEFAULT_PAGE_SIZE);
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

  private void update(Long slotNoCheckpoint, Long currentMaxSlotNo) {
    log.info("Start update TokenTxCount");
    long startTime = System.currentTimeMillis();

    List<String> unitsInBlockRange =
        addressTxAmountRepository.findUnitBySlotInRange(slotNoCheckpoint, currentMaxSlotNo);

    log.info(
        "unitsInBlockRange from blockCheckpoint {} to {}, size: {}",
        slotNoCheckpoint,
        currentMaxSlotNo,
        unitsInBlockRange.size());

    List<CompletableFuture<List<TokenTxCount>>> tokenTxCountNeedSaveFutures = new ArrayList<>();

    BatchUtils.doBatching(
        100,
        unitsInBlockRange,
        units -> {
          tokenTxCountNeedSaveFutures.add(
              tokenInfoServiceAsync
                  .buildTokenTxCountList(units)
                  .exceptionally(
                      e -> {
                        throw new RuntimeException(
                            "Failed to build token tx count list for units: " + units, e);
                      }));

          // After every 10 batches, insert the fetched token tx count data into the database in
          // batches.
          if (tokenTxCountNeedSaveFutures.size() % 10 == 0) {
            var tokenTxCountList =
                tokenTxCountNeedSaveFutures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .toList();

            BatchUtils.doBatching(1000, tokenTxCountList, jooqTokenTxCountRepository::insertAll);
            tokenTxCountNeedSaveFutures.clear();
          }
        });

    // Insert the remaining token tx count data into the database.
    var tokenTxCountList =
        tokenTxCountNeedSaveFutures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .toList();
    BatchUtils.doBatching(1000, tokenTxCountList, jooqTokenTxCountRepository::insertAll);

    log.info(
        "End update TokenTxCount with size = {} in {} ms",
        unitsInBlockRange.size(),
        System.currentTimeMillis() - startTime);
  }
}
