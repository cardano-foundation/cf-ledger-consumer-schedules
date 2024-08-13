package org.cardanofoundation.job.schedules;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.cardanofoundation.explorer.common.entity.explorer.BlockStatisticsDaily;
import org.cardanofoundation.explorer.common.entity.explorer.BlockStatisticsPerEpoch;
import org.cardanofoundation.job.repository.explorer.jooq.JOOQBlockStatisticsDailyRepository;
import org.cardanofoundation.job.repository.explorer.jooq.JOOQBlockStatisticsPerEpochRepository;

@Service
@RequiredArgsConstructor
@Log4j2
@ConditionalOnProperty(
    value = "jobs.block-statistics.enabled",
    matchIfMissing = true,
    havingValue = "true")
public class BlockStatisticsSchedule {
  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${jobs.block-statistics.daily-url}")
  private String apiBlockStatisticsDailyUrl;

  @Value("${jobs.block-statistics.per-epoch-url}")
  private String apiBlockStatisticsPerEpochUrl;

  private final JOOQBlockStatisticsDailyRepository jooqBlockStatisticsDailyRepository;

  private final JOOQBlockStatisticsPerEpochRepository jooqBlockStatisticsPerEpochRepository;

  @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
  // midnight
  @Transactional(value = "explorerTransactionManager")
  void getBlockStatisticsPerEpoch() {
    long startTime = System.currentTimeMillis();
    log.info("Get Block Statistics Per Epoch Schedule");
    String blockStatisticsStr =
        restTemplate.getForEntity(apiBlockStatisticsPerEpochUrl, String.class).getBody();
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    try {
      List<BlockStatisticsPerEpoch> blockStatisticsPerEpochList =
          mapper.readValue(
              blockStatisticsStr, new TypeReference<List<BlockStatisticsPerEpoch>>() {});
      if (blockStatisticsPerEpochList.isEmpty()) {
        log.info("Data of per-epoch block statistics is empty");
      } else {
        jooqBlockStatisticsPerEpochRepository.insertAll(blockStatisticsPerEpochList);
        log.info(
            "Data of per-epoch block statistics has been upserted, taken time: {} ms",
            System.currentTimeMillis() - startTime);
      }
    } catch (JsonProcessingException e) {
      log.error("Error parsing data of block statistics per epoch: {}", e.getMessage());
    } catch (Exception e) {
      log.error("Unknown error: {}", e.getMessage());
    }
  }

  @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
  // midnight
  @Transactional(value = "explorerTransactionManager")
  void getBlockStatisticsDaily() {
    long startTime = System.currentTimeMillis();
    log.info("Get Block Statistics Daily Schedule");
    String blockStatisticsStr =
        restTemplate.getForEntity(apiBlockStatisticsDailyUrl, String.class).getBody();
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    try {
      List<BlockStatisticsDaily> blockStatisticsList =
          mapper.readValue(blockStatisticsStr, new TypeReference<List<BlockStatisticsDaily>>() {});
      if (blockStatisticsList.isEmpty()) {
        log.info("Data of daily block statistics is empty");
      } else {
        jooqBlockStatisticsDailyRepository.insertAll(blockStatisticsList);
        log.info(
            "Data of daily block statistics has been upserted, taken time: {} ms",
            System.currentTimeMillis() - startTime);
      }
    } catch (JsonProcessingException e) {
      log.error("Error parsing data of block statistics daily: {}", e.getMessage());
    } catch (Exception e) {
      log.error("Unknown error: {}", e.getMessage());
    }
  }
}
