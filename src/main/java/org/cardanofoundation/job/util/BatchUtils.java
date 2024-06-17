package org.cardanofoundation.job.util;

import java.util.List;
import java.util.function.Consumer;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class BatchUtils {

  /**
   * Consume collection data per batch size
   */
  public static <T> void doBatching(int batchSize,
                                    List<T> collection,
                                    Consumer<List<T>> consumer) {
    if (collection == null || collection.isEmpty()) {
      return;
    }

    log.info("Start batch processing with collection size: [{}], batchSize: [{}]",
             collection.size(), batchSize);

    final int COLLECTION_SIZE = collection.size();
    long start = System.currentTimeMillis();
    for (int startBatchIdx = 0; startBatchIdx < COLLECTION_SIZE; startBatchIdx += batchSize) {
      int endBatchIdx = Math.min(startBatchIdx + batchSize, COLLECTION_SIZE);
      log.info("batch processing from element number: {} - {}", startBatchIdx, endBatchIdx - 1);
      final List<T> batchList = collection.subList(startBatchIdx, endBatchIdx);
      consumer.accept(batchList);
    }
    log.info("Completed batch processing in: {} ms", (System.currentTimeMillis() - start));
  }


}
