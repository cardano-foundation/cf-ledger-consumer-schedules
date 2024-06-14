package org.cardanofoundation.job.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class BatchUtils {

  /** Consume collection data per batch size */
  public static <T> void doBatching(int batchSize, List<T> collection, Consumer<List<T>> consumer) {
    if (collection == null || collection.isEmpty()) {
      return;
    }

    List<CompletableFuture<Void>> acceptCompletableFutures = new ArrayList<>();
    log.info(
        "Start batch processing with collection size: [{}], batchSize: [{}]",
        collection.size(),
        batchSize);

    final int COLLECTION_SIZE = collection.size();
    long start = System.currentTimeMillis();
    for (int startBatchIdx = 0; startBatchIdx < COLLECTION_SIZE; startBatchIdx += batchSize) {
      int endBatchIdx = Math.min(startBatchIdx + batchSize, COLLECTION_SIZE);
      final List<T> batchList = collection.subList(startBatchIdx, endBatchIdx);
      acceptCompletableFutures.add(CompletableFuture.runAsync(() -> consumer.accept(batchList)));

      if (acceptCompletableFutures.size() % 5 == 0) {
        CompletableFuture.allOf(acceptCompletableFutures.toArray(new CompletableFuture[0])).join();
        acceptCompletableFutures.clear();
      }
    }

    CompletableFuture.allOf(acceptCompletableFutures.toArray(new CompletableFuture[0])).join();
    acceptCompletableFutures.clear();
    log.info("Completed batch processing in: {} ms", (System.currentTimeMillis() - start));
  }
}
