package org.cardanofoundation.job.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLHandshakeException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandshakeTimeoutException;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.cardanofoundation.job.constant.JobConstants;
import org.cardanofoundation.job.dto.PoolData;
import org.cardanofoundation.job.event.message.FetchPoolDataFail;
import org.cardanofoundation.job.event.message.FetchPoolDataSuccess;
import org.cardanofoundation.job.projection.PoolHashUrlProjection;
import org.cardanofoundation.job.repository.PoolHashRepository;
import org.cardanofoundation.job.service.PoolOfflineDataFetchingService;
import org.cardanofoundation.ledgersync.common.util.UrlUtil;
import reactor.netty.http.client.HttpClient;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.MOVED_PERMANENTLY;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class PoolOfflineDataFetchingServiceImpl implements PoolOfflineDataFetchingService {

  final PoolHashRepository poolHashRepository;
  final WebClient.Builder webClientBuilder;
  final ApplicationEventPublisher applicationEventPublisher;

  static final int TIMEOUT = 30000;
  static final int READ_TIMEOUT = 19000;
  static final int WRITE_TIMEOUT = 10000;
  static final int LIMIT_BYTES = 512;

  /**
   * Insert PoolOfflineData and PoolOfflineFetchError with batch with Map object key is hash String
   * and value is PoolOfflineData | PoolOfflineFetchError If map size equal to batch size,
   * PoolOfflineData would be committed
   *
   * @param start start position
   */
  @Override
  public int fetchBatch(Integer start) {
    int fetchSize = 0;
    while (true) {
      List<PoolHashUrlProjection> poolHashUrlProjections =
          poolHashRepository.findPoolHashAndUrl(PageRequest.of(start, JobConstants.DEFAULT_BATCH));

      fetchSize = fetchSize + poolHashUrlProjections.size();
      if (CollectionUtils.isEmpty(poolHashUrlProjections)) {
        break;
      }

      poolHashUrlProjections.forEach(this::fetchData);
      start = start + 1;
    }

    return fetchSize;
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
              .startTls(true)
              .build();

      var httpClient =
          HttpClient.create()
              .wiretap(Boolean.FALSE)
              .secure(t -> t.sslContext(sslContext))
              .followRedirect(Boolean.TRUE)
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIMEOUT)
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
          .timeout(Duration.ofMillis(TIMEOUT))
          .doOnError(SSLHandshakeException.class, throwable -> fetchFail("", poolHash, throwable))
          .doOnError(SslHandshakeTimeoutException.class,
              throwable -> fetchFail("", poolHash, throwable))
          .doOnError(DecoderException.class, throwable -> fetchFail("", poolHash, throwable))
          .doOnError(Exception.class, throwable -> fetchFail("", poolHash, throwable))
          .map(
              response -> {
                HttpStatusCode statusCode = response.getStatusCode();
                if (statusCode.equals(NOT_FOUND)) {
                  fetchFail("Not Found ", poolHash);
                  return Optional.empty();
                } else if (statusCode.equals(FORBIDDEN)) {
                  log.error("FORBIDDEN for url: {}", poolHash.getUrl());
                  return Optional.empty();
                } else if (statusCode.equals(REQUEST_TIMEOUT)) {
                  log.error("REQUEST_TIMEOUT for url: {}", poolHash.getUrl());
                  return Optional.empty();
                } else if (statusCode.equals(MOVED_PERMANENTLY)) {
                  log.error("MOVED PERMANENTLY for url: {}", poolHash.getUrl());
                  return Optional.empty();
                } else if (statusCode.equals(OK)) {
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

                  if (Objects.requireNonNull(response.getBody()).getBytes().length > LIMIT_BYTES) {
                    return Optional.empty();
                  }
                  return Optional.of(response);
                }
                log.info(
                    "unhandled code {} for url: {}", response.getStatusCode(), poolHash.getUrl());
                return Optional.of(response);
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
                                  .status(OK.value())
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
                              "Response larger than 512 bytes or response body not in json with url",
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
            .errorMessage(String.format("%s %s", error, poolHash.getUrl()))
            .poolId(poolHash.getPoolId())
            .metadataRefId(poolHash.getMetadataId())
            .build();

    log.error("{} {}", error, poolHash.getUrl());
    applicationEventPublisher.publishEvent(new FetchPoolDataFail(data));
  }

  private void fetchFail(String error, PoolHashUrlProjection poolHash, Throwable throwable) {
    PoolData data =
        PoolData.builder()
            .errorMessage(String.format("%s %s", poolHash.getUrl(), throwable.getMessage()))
            .poolId(poolHash.getPoolId())
            .metadataRefId(poolHash.getMetadataId())
            .build();

    log.error("{} {} {}", error, poolHash.getUrl(), throwable.getMessage());
    applicationEventPublisher.publishEvent(new FetchPoolDataFail(data));
  }
}
