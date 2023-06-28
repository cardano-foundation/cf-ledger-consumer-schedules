package org.cardanofoundation.job.schedules;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.gson.*;

import org.cardanofoundation.explorer.consumercommon.entity.MultiAsset_;
import org.cardanofoundation.job.common.enumeration.RedisKey;
import org.cardanofoundation.job.service.TokenService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenListCacheSchedule {

  private static final Gson GSON =
      new GsonBuilder()
          .registerTypeAdapter(
              LocalDate.class,
              (JsonSerializer<LocalDate>)
                  (value, type, context) ->
                      new JsonPrimitive(value.format(DateTimeFormatter.ISO_LOCAL_DATE)))
          .registerTypeAdapter(
              LocalDateTime.class,
              (JsonSerializer<LocalDateTime>)
                  (value, type, context) ->
                      new JsonPrimitive(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
          .registerTypeAdapter(
              LocalDate.class,
              (JsonDeserializer<LocalDate>)
                  (jsonElement, type, context) ->
                      LocalDate.parse(
                          jsonElement.getAsJsonPrimitive().getAsString(),
                          DateTimeFormatter.ISO_LOCAL_DATE))
          .registerTypeAdapter(
              LocalDateTime.class,
              (JsonDeserializer<LocalDateTime>)
                  (jsonElement, type, context) ->
                      LocalDateTime.parse(
                          jsonElement.getAsJsonPrimitive().getAsString(),
                          DateTimeFormatter.ISO_LOCAL_DATE_TIME))
          .create();

  private final RedisTemplate<String, Object> redisTemplate;
  private final TokenService tokenService;

  @Value("${application.network}")
  private String network;

  @Value("${jobs.token-page-cache.ttl}")
  private int cacheTimeToLive;

  @Scheduled(fixedDelayString = "${jobs.token-page-cache.fixed-delay}")
  public void buildCacheFrequentlyCalledTokenPageApi() {
    long start = System.currentTimeMillis();
    List<Pageable> listPageable =
        List.of(
            PageRequest.of(0, 50, Sort.Direction.DESC, MultiAsset_.TX_COUNT),
            PageRequest.of(1, 50, Sort.Direction.DESC, MultiAsset_.TX_COUNT),
            PageRequest.of(2, 50, Sort.Direction.DESC, MultiAsset_.TX_COUNT),
            PageRequest.of(0, 100, Sort.Direction.DESC, MultiAsset_.TX_COUNT),
            PageRequest.of(1, 100, Sort.Direction.DESC, MultiAsset_.TX_COUNT),
            PageRequest.of(2, 100, Sort.Direction.DESC, MultiAsset_.TX_COUNT));
    listPageable.forEach(
        pageable ->
            redisTemplate
                .opsForValue()
                .set(
                    RedisKey.REDIS_TOKEN_PAGE.name() + ":" + network + ":" + toStr(pageable),
                    GSON.toJson(tokenService.filterToken(pageable)),
                    cacheTimeToLive,
                    TimeUnit.MILLISECONDS));

    log.info(
        "Build cache for frequently called Token Page API successfully, takes: [{} ms]",
        (System.currentTimeMillis() - start));
  }

  private String toStr(Pageable pageable) {
    return pageable.toString().replace(" ", "").replace(":", "_");
  }
}
