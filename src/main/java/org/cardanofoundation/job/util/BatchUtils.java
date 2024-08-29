package org.cardanofoundation.job.util;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

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

    log.info(
        "Start batch processing with collection size: [{}], batchSize: [{}]",
        collection.size(),
        batchSize);

    final int COLLECTION_SIZE = collection.size();
    long start = System.currentTimeMillis();
    for (int startBatchIdx = 0; startBatchIdx < COLLECTION_SIZE; startBatchIdx += batchSize) {
      int endBatchIdx = Math.min(startBatchIdx + batchSize, COLLECTION_SIZE);
      final List<T> batchList = collection.subList(startBatchIdx, endBatchIdx);
      consumer.accept(batchList);
    }
    log.info("Completed batch processing in: {} ms", (System.currentTimeMillis() - start));
  }

  public static <T> void processInBatches(
      int batchSize,
      List<T> itemList,
      Function<List<T>, CompletableFuture<List<T>>> processingFunction,
      Consumer<List<T>> saveFunction,
      String jobName) {

    long startTime = System.currentTimeMillis();
    List<CompletableFuture<List<T>>> futures =
        IntStream.range(0, (itemList.size() + batchSize - 1) / batchSize)
            .mapToObj(
                i ->
                    itemList.subList(i * batchSize, Math.min((i + 1) * batchSize, itemList.size())))
            .map(processingFunction)
            .toList();

    CompletableFuture<List<T>> allFutures =
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(
                v ->
                    futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .toList());

    try {
      List<T> processedList = allFutures.get();
      saveFunction.accept(processedList);
      log.info(
          "Batch processing completed in: {} ms -- JobName: [{}]",
          (System.currentTimeMillis() - startTime),
          jobName);
    } catch (InterruptedException | ExecutionException e) {
      log.error(
          "Error occurred during batch processing: {} -- JobName: [{}]", e.getMessage(), jobName);
    }
  }
}
