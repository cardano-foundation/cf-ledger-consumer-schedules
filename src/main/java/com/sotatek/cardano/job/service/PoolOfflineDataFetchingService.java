package com.sotatek.cardano.job.service;

import java.math.BigInteger;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLHandshakeException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandshakeTimeoutException;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

import com.sotatek.cardano.job.constant.JobConstants;
import com.sotatek.cardano.job.dto.PoolData;
import com.sotatek.cardano.job.event.FetchPoolDataFail;
import com.sotatek.cardano.job.event.FetchPoolDataSuccess;
import com.sotatek.cardano.job.projection.PoolHashUrlProjection;
import com.sotatek.cardano.job.repository.PoolHashRepository;
import com.sotatek.cardano.ledgersync.util.UrlUtil;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class PoolOfflineDataFetchingService {

  final PoolHashRepository poolHashRepository;
  final WebClient.Builder webClientBuilder;
  final ApplicationEventPublisher applicationEventPublisher;

  static final int TIMEOUT = 30000;
  static final int READ_TIMEOUT = 19000;
  static final int WRITE_TIMEOUT = 10000;
  static final int LIMIT_BYTES = 512;

  @Scheduled(fixedDelayString = "${jobs.fetch-pool-offline-data.delay}")
  public void updatePoolOffline() {
    fetchBatch(BigInteger.ZERO.longValue());
  }

  /**
   * Insert PoolOfflineData and PoolOfflineFetchError with batch with Map object key is hash String
   * and value is PoolOfflineData | PoolOfflineFetchError If map size equal to batch size,
   * PoolOfflineData would be committed
   *
   * @param start start position
   */
  public void fetchBatch(Long start) {

    AtomicReference<Long> startReference = new AtomicReference<>(start);
    while (true) {
      List<PoolHashUrlProjection> poolHashUrlProjections =
          poolHashRepository.findPoolHashAndUrl(
              PageRequest.of(BigInteger.ZERO.intValue(), JobConstants.DEFAULT_BATCH));

      if (CollectionUtils.isEmpty(poolHashUrlProjections)) {
        break;
      }

      poolHashUrlProjections.forEach(this::fetchData);

      startReference.set(poolHashUrlProjections.get(poolHashUrlProjections.size() - 1).getPoolId());
    }
  }

  private void fetchData(PoolHashUrlProjection poolHash) {

    if (!UrlUtil.isUrl(poolHash.getUrl())) {
      fetchFail("not valid url may contain special character", poolHash);
      return;
    }

    try {
      var sslContext =
          SslContextBuilder.forClient()
              .sslProvider(SslProvider.JDK)
              .trustManager(InsecureTrustManagerFactory.INSTANCE)
              .build();

      var httpClient =
          HttpClient.create()
              .wiretap(Boolean.FALSE)
              .secure(t -> t.sslContext(sslContext))
              .followRedirect(Boolean.TRUE)
              .responseTimeout(Duration.ofMillis(TIMEOUT))
              .doOnConnected(
                  connection -> {
                    connection.addHandlerFirst(
                        new ReadTimeoutHandler(READ_TIMEOUT, TimeUnit.MILLISECONDS));
                    connection.addHandlerFirst(
                        new WriteTimeoutHandler(WRITE_TIMEOUT, TimeUnit.MILLISECONDS));
                  });

      webClientBuilder
          .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .clientConnector(new ReactorClientHttpConnector(httpClient))
          .build()
          .get()
          .uri(UrlUtil.formatSpecialCharactersUrl(poolHash.getUrl()))
          .acceptCharset(StandardCharsets.UTF_8)
          .retrieve()
          .toEntity(String.class)
          .doOnError(SSLHandshakeException.class, e -> fetchFail("SSL handshake fail %s", poolHash))
          .doOnError(
              SslHandshakeTimeoutException.class,
              e -> fetchFail("SSL handshake time out %s", poolHash))
          .doOnError(ClosedChannelException.class, throwable -> fetchFail("Time out %s", poolHash))
          .map(
              response -> {
                switch (response.getStatusCode()) {
                  case NOT_FOUND:
                    fetchFail("Not Found ", poolHash);
                    return Optional.empty();

                  case FORBIDDEN:
                    log.error("FORBIDDEN for url: {}", poolHash.getUrl());
                    return Optional.empty();
                  case REQUEST_TIMEOUT:
                    log.error("REQUEST_TIMEOUT for url: {}", poolHash.getUrl());
                    return Optional.empty();
                  case MOVED_PERMANENTLY:
                    log.error("MOVED PERMANENTLY for url: {}", poolHash.getUrl());
                    return Optional.empty();
                  case OK:
                    if (Objects.requireNonNull(response.getHeaders().get(HttpHeaders.CONTENT_TYPE))
                        .stream()
                        .noneMatch(
                            contentType ->
                                contentType.contains(MediaType.APPLICATION_JSON_VALUE)
                                    || contentType.contains(MediaType.TEXT_PLAIN_VALUE)
                                    || contentType.contains(
                                        MediaType.APPLICATION_OCTET_STREAM_VALUE))) {
                      return Optional.empty();
                    }

                    if (Objects.requireNonNull(response.getBody()).getBytes().length
                        > LIMIT_BYTES) {
                      return Optional.empty();
                    }
                    return Optional.of(response);

                  default:
                    log.info(
                        "unhandled code {} for url: {}",
                        response.getStatusCode(),
                        poolHash.getUrl());
                    return Optional.of(response);
                }
              })
          .toFuture()
          .thenAccept(
              responseOptional ->
                  responseOptional.ifPresentOrElse(
                      response -> {
                        ResponseEntity responseEntity = (ResponseEntity) response;

                        var responseBody = String.valueOf(responseEntity.getBody());

                        if (Objects.nonNull(responseBody)) {
                          PoolData data =
                              PoolData.builder()
                                  .status(HttpStatus.OK.value())
                                  .json(responseBody.getBytes(StandardCharsets.UTF_8))
                                  .poolId(poolHash.getPoolId())
                                  .metadataRefId(poolHash.getMetadataId())
                                  .hash(
                                      HexUtil.encodeHexString(
                                          Blake2bUtil.blake2bHash256(responseBody.getBytes())))
                                  .build();
                          applicationEventPublisher.publishEvent(new FetchPoolDataSuccess(data));
                        }
                      },
                      () ->
                          fetchFail(
                              "Response larger than 512 bytes or response body not in json with",
                              poolHash)));
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

  /**
   * Logging and send event for fetching pool data fail
   *
   * @param error log content
   */
  private void fetchFail(String error, PoolHashUrlProjection poolHash) {
    PoolData data =
        PoolData.builder()
            .errorMessage(String.format(error, poolHash.getUrl()))
            .poolId(poolHash.getPoolId())
            .metadataRefId(poolHash.getMetadataId())
            .build();

    log.error("{} {}", error, poolHash.getUrl());
    applicationEventPublisher.publishEvent(new FetchPoolDataFail(data));
  }
}
