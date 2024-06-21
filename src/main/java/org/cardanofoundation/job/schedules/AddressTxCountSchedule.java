package org.cardanofoundation.job.schedules;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.job.repository.ledgersyncagg.jooq.JOOQAddressTxCountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class AddressTxCountSchedule {

    private final JdbcTemplate jdbcTemplate;

    @Value("${application.network}")
    private String network;


    @Value("${jobs.address-tx-count.insert-batch-size}")
    private int insertBatchSize;

    @Value("${jobs.address-tx-count.number-of-threads}")
    private int numberOfThreads;

    private final JOOQAddressTxCountRepository jooqAddressTxCountRepository;

    @Scheduled(fixedDelayString = "${jobs.address-tx-count.fixed-delay}")
    @Transactional
    public void syncAddressTxCount() {
        log.info("Start init AddressTxCount");
        long startTime = System.currentTimeMillis();

        Long totalAddresses = jdbcTemplate.queryForObject("SELECT max(id) FROM preprod_aggregation.address", Long.class);
        if (totalAddresses == null) {
            return;
        }

        long partitionSize = totalAddresses / numberOfThreads;

        runParallel(partitionSize, insertBatchSize, numberOfThreads, totalAddresses);

        log.info("End init AddressTxCount in {} ms", System.currentTimeMillis() - startTime);
    }

    @SneakyThrows
    private void runParallel(long partitionSize, int batchSize, int numberOfThreads, long totalAddresses) {

        List<CompletableFuture> futures = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            long startId = (i * partitionSize) + 1;  // ID starts from 1
            long endId = (i == numberOfThreads - 1) ? totalAddresses : startId + partitionSize - 1; // ensure endId does not exceed totalAddresses

            CompletableFuture<Boolean> completableFuture = CompletableFuture.supplyAsync(() -> {
                jooqAddressTxCountRepository.insertAddressTxCount(startId, endId, batchSize);
                return true;
            });

            futures.add(completableFuture);
        }


        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        for (var future : futures) {
            future.get();
        }
    }
}
