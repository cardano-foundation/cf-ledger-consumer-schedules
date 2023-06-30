package org.cardanofoundation.job.service.impl;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.MOVED_PERMANENTLY;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.net.ssl.SSLException;
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
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandshakeTimeoutException;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

import org.cardanofoundation.job.constant.JobConstants;
import org.cardanofoundation.job.dto.PoolData;
import org.cardanofoundation.job.event.message.FetchPoolDataFail;
import org.cardanofoundation.job.event.message.FetchPoolDataSuccess;
import org.cardanofoundation.job.projection.PoolHashUrlProjection;
import org.cardanofoundation.job.repository.PoolHashRepository;
import org.cardanofoundation.job.service.PoolOfflineDataFetchingService;
import org.cardanofoundation.ledgersync.common.util.UrlUtil;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class PoolOfflineDataFetchingServiceImpl implements PoolOfflineDataFetchingService {

  public static final String EXTENDED = "extended";
  public static final String INFO = "info";
  public static final String URL_PNG_LOGO = "url_png_logo";
  public static final String URL_PNG_ICON_64_X_64 = "url_png_icon_64x64";
  public static final int URL_LIMIT = 2000;
  final PoolHashRepository poolHashRepository;
  final WebClient.Builder webClientBuilder;
  final ApplicationEventPublisher applicationEventPublisher;
  final ObjectMapper objectMapper;

  static final int TIMEOUT = 30000;
  static final int READ_TIMEOUT = 19000;
  static final int WRITE_TIMEOUT = 10000;
  static final int LIMIT_BYTES = 512;

  @Override
  public int fetchPoolOfflineDataByBatch(Integer start) {
    int fetchSize = 0;

    while (true) {
      List<PoolHashUrlProjection> poolHashUrlProjections =
          poolHashRepository.findPoolHashAndUrl(PageRequest.of(start, JobConstants.DEFAULT_BATCH));

      fetchSize = fetchSize + poolHashUrlProjections.size();
      if (CollectionUtils.isEmpty(poolHashUrlProjections)) {
        break;
      }

      poolHashUrlProjections.forEach(this::fetchPoolOffLineMetaData);

      start = start + 1;
    }

    return fetchSize;
  }

  @Override
  public void fetchPoolOfflineDataLogo(Stream<PoolData> stream) {
    stream.forEach(
        poolData -> {
          // Sleep 12 millis second for fetch data can fill to stream
          try {
            Thread.sleep(20);
          } catch (InterruptedException e) {
            log.error(e.getMessage());
          }
          // set empty string element to null
          if (ObjectUtils.isEmpty(poolData.getJson())) {
            fetchFail("Empty json ", poolData);
            poolData.setValid(Boolean.FALSE);
            return;
          }

          String json = new String(poolData.getJson());

          try {
            final Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            // set empty json element to null
            if (CollectionUtils.isEmpty(map)) {
              fetchFail("Data is empty", poolData);
              poolData.setValid(Boolean.FALSE);
              return;
            }

            // if pool metadata have no extended field then skip
            if (map.containsKey(EXTENDED)) {
              final var poolExtendedUrl = String.valueOf(map.get(EXTENDED));

              if (!UrlUtil.isUrl(poolExtendedUrl)) {
                fetchFail("not valid url may contain special character", poolData);
                return;
              }

              log.debug("fetch extend url {}", poolExtendedUrl);

              final var processedPoolData = poolData;

              buildWebClient()
                  .get()
                  .uri(UrlUtil.formatSpecialCharactersUrl(poolExtendedUrl))
                  .acceptCharset(StandardCharsets.UTF_8)
                  .retrieve()
                  .toEntity(String.class)
                  .timeout(Duration.ofMillis(TIMEOUT))
                  .doOnError(
                      SSLHandshakeException.class,
                      throwable -> fetchFail(throwable.getMessage(), processedPoolData))
                  .doOnError(
                      SslHandshakeTimeoutException.class,
                      throwable -> fetchFail(throwable.getMessage(), processedPoolData))
                  .doOnError(
                      DecoderException.class,
                      throwable -> fetchFail(throwable.getMessage(), processedPoolData))
                  .doOnError(
                      Exception.class,
                      throwable -> fetchFail(throwable.getMessage(), processedPoolData))
                  .toFuture()
                  .thenAccept(
                      responsePoolExtended -> {
                        if (Objects.isNull(responsePoolExtended)
                            || ObjectUtils.isEmpty(responsePoolExtended.getBody())) {
                          return;
                        }

                        String extendBody = responsePoolExtended.getBody();
                        // make another try catch for catching extendMap only and not affect pool
                        try {
                          final Map<String, Object> extendMap =
                              objectMapper.readValue(extendBody, new TypeReference<>() {});
                          if (CollectionUtils.isEmpty(map)) {
                            return;
                          }

                          findExtendedLogoWithLoop(extendMap, processedPoolData);
                        } catch (JsonProcessingException e) {
                          log.debug(
                              "Extended of metadata ref {} fail with content",
                              processedPoolData.getMetadataRefId(),
                              extendBody);
                        }
                      });
            }
          } catch (JsonProcessingException e) {
            fetchFail(e.getMessage(), poolData);
            poolData.setValid(Boolean.FALSE);
          } catch (Exception e) {
            log.debug(e.getMessage());
          }
        });
  }

  /**
   * Find URL_PNG_LOGO and URL_PNG_ICON_64_X_64 in extend map
   *
   * @param map
   * @param poolData
   */
  private void findExtendedLogoWithLoop(Map<String, Object> map, PoolData poolData) {
    map.keySet()
        .forEach(
            key -> {
              if (map.get(key) instanceof LinkedHashMap<?, ?> subMap) {
                findExtendedLogoWithLoop((LinkedHashMap<String, Object>) subMap, poolData);
                return;
              }

              if (key.equals(URL_PNG_LOGO)) {
                AtomicReference<String> urlLogo = new AtomicReference<>("");
                Optional.ofNullable(map.get(URL_PNG_LOGO))
                    .ifPresent(url -> urlLogo.set(String.valueOf(url)));
                if (urlLogo.get().length() < URL_LIMIT && UrlUtil.isUrl(urlLogo.get())) {
                  poolData.setLogoUrl(urlLogo.get());
                }
              }

              if (key.equals(URL_PNG_ICON_64_X_64)) {
                AtomicReference<String> urlIcon = new AtomicReference<>("");
                Optional.ofNullable(map.get(URL_PNG_ICON_64_X_64))
                    .ifPresent(url -> urlIcon.set(String.valueOf(url)));

                if (urlIcon.get().length() < URL_LIMIT && UrlUtil.isUrl(urlIcon.get())) {
                  poolData.setIconUrl(urlIcon.get());
                }
              }
            });
  }

  /**
   * Fetch asynchronous pool metadata then send to success or fail queue.
   *
   * @param poolHash
   */
  private void fetchPoolOffLineMetaData(PoolHashUrlProjection poolHash) {

    if (!UrlUtil.isUrl(poolHash.getUrl())) {
      fetchFail("not valid url may contain special character", poolHash);
      return;
    }

    try {
      buildWebClient()
          .get()
          .uri(UrlUtil.formatSpecialCharactersUrl(poolHash.getUrl()))
          .acceptCharset(StandardCharsets.UTF_8)
          .retrieve()
          .toEntity(String.class)
          .timeout(Duration.ofMillis(TIMEOUT))
          .doOnError(SSLHandshakeException.class, throwable -> fetchFail("", poolHash, throwable))
          .doOnError(
              SslHandshakeTimeoutException.class, throwable -> fetchFail("", poolHash, throwable))
          .doOnError(DecoderException.class, throwable -> fetchFail("", poolHash, throwable))
          .doOnError(Exception.class, throwable -> fetchFail("", poolHash, throwable))
          .map(
              response -> {
                HttpStatusCode statusCode = response.getStatusCode();
                if (statusCode.equals(NOT_FOUND)) {
                  fetchFail("Not Found ", poolHash);
                  return Optional.empty();
                } else if (statusCode.equals(FORBIDDEN)) {
                  fetchFail("FORBIDDEN ", poolHash);
                  log.debug("FORBIDDEN for url: {}", poolHash.getUrl());
                  return Optional.empty();
                } else if (statusCode.equals(REQUEST_TIMEOUT)) {
                  fetchFail("REQUEST TIMEOUT ", poolHash);
                  log.debug("REQUEST TIMEOUT for url: {}", poolHash.getUrl());
                  return Optional.empty();
                } else if (statusCode.equals(MOVED_PERMANENTLY)) {
                  fetchFail("MOVED PERMANENTLY ", poolHash);
                  log.debug("MOVED PERMANENTLY for url: {}", poolHash.getUrl());
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
                log.debug(
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
      log.debug("{} {}", e.getMessage(), poolHash.getUrl());
      fetchFail(e.getMessage(), poolHash);
    }
  }

  /**
   * Build webclient with SSL certificate provided by JDK
   *
   * @return webclient
   * @throws SSLException
   */
  private WebClient buildWebClient() throws SSLException {

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

    return webClientBuilder
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
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

    log.debug("{} {}", error, poolHash.getUrl());
    applicationEventPublisher.publishEvent(new FetchPoolDataFail(data));
  }

  /**
   * Logging and send event for fetched pool offline data
   *
   * @param error
   * @param poolHash
   */
  private void fetchFail(String error, PoolData poolHash) {
    PoolData data =
        PoolData.builder()
            .errorMessage(
                String.format("%s %s %s", error, poolHash.getPoolId(), poolHash.getMetadataRefId()))
            .poolId(poolHash.getPoolId())
            .metadataRefId(poolHash.getMetadataRefId())
            .build();

    log.debug("{} {} {}", error, poolHash.getPoolId(), poolHash.getMetadataRefId());
    applicationEventPublisher.publishEvent(new FetchPoolDataFail(data));
  }

  /**
   * Logging and send event for fetching pool data fail
   *
   * @param error log content
   */
  private void fetchFail(String error, PoolHashUrlProjection poolHash, Throwable throwable) {
    PoolData data =
        PoolData.builder()
            .errorMessage(String.format("%s %s", poolHash.getUrl(), throwable.getMessage()))
            .poolId(poolHash.getPoolId())
            .metadataRefId(poolHash.getMetadataId())
            .build();

    log.debug("{} {} {}", error, poolHash.getUrl(), throwable.getMessage());
    applicationEventPublisher.publishEvent(new FetchPoolDataFail(data));
  }
}
