package com.sotatek.cardano.job.service;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import com.sotatek.cardano.common.entity.PoolOfflineData;
import com.sotatek.cardano.common.entity.PoolOfflineFetchError;
import com.sotatek.cardano.job.constant.JobConstants;
import com.sotatek.cardano.job.dto.PoolData;
import com.sotatek.cardano.job.event.FetchPoolDataFail;
import com.sotatek.cardano.job.event.FetchPoolDataSuccess;
import com.sotatek.cardano.job.projection.PoolHashUrlProjection;
import com.sotatek.cardano.job.repository.PoolHashRepository;
import com.sotatek.cardano.job.repository.PoolOfflineDataRepository;
import com.sotatek.cardano.job.repository.PoolOfflineFetchErrorRepository;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandshakeTimeoutException;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.math.BigInteger;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class FetchingPoolOfflineDataService {

  final PoolHashRepository poolHashRepository;
  final PoolOfflineDataRepository poolOfflineDataRepository;
  final PoolOfflineFetchErrorRepository poolOfflineFetchErrorRepository;
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
   * and value is  PoolOfflineData | PoolOfflineFetchError If map size equal to batch size,
   * PoolOfflineData would be committed
   *
   * @param start start position
   */
  public void fetchBatch(Long start) {

    AtomicReference<Long> startReference = new AtomicReference<>(start);
    while (true) {
      List<PoolHashUrlProjection> poolHashUrlProjections = poolHashRepository.findPoolHashAndUrl(
          startReference.get(),
          PageRequest.of(BigInteger.ZERO.intValue(),
              JobConstants.DEFAULT_BATCH));

      if (CollectionUtils.isEmpty(poolHashUrlProjections)) {
        break;
      }

      poolHashUrlProjections
          .forEach(
              this::fetchData);

      startReference.set(poolHashUrlProjections.get(poolHashUrlProjections.size() - 1).getPoolId());
    }
  }


  private void fetchData(PoolHashUrlProjection poolHash) {

    if (!isUrl(poolHash.getUrl())) {
      PoolData data = PoolData.builder()
          .status(HttpStatus.NOT_FOUND.value())
          .errorMessage(
              String.format("not valid url may contain unscene character  %s", poolHash.getUrl()))
          .poolId(poolHash.getPoolId())
          .metadataRefId(poolHash.getMetadataId())
          .build();

      applicationEventPublisher.publishEvent(new FetchPoolDataFail(data));
      return;
    }

    try {
      var sslContext = SslContextBuilder
          .forClient()
          .sslProvider(SslProvider.JDK)
          .trustManager(InsecureTrustManagerFactory.INSTANCE)
          .build();

      var httpClient = HttpClient.create()
          //.wiretap("reactor.netty.http.client.HttpClient", LogLevel., AdvancedByteBufFormat.TEXTUAL, StandardCharsets.UTF_8)
          .wiretap(Boolean.FALSE)
          .secure(t -> t.sslContext(sslContext))
          .followRedirect(Boolean.TRUE)
          .responseTimeout(Duration.ofMillis(TIMEOUT))
          .doOnConnected(connection -> {
            connection.addHandlerFirst(new ReadTimeoutHandler(READ_TIMEOUT, TimeUnit.MILLISECONDS));
            connection.addHandlerFirst(
                new WriteTimeoutHandler(WRITE_TIMEOUT, TimeUnit.MILLISECONDS));

          });

      webClientBuilder
          .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .clientConnector(new ReactorClientHttpConnector(httpClient))
          .build()
          .get()
          .uri(poolHash.getUrl())
          //.uri("https://raw.githubusercontent.com/hodlonaut/a/master/t1.json")
          .acceptCharset(StandardCharsets.UTF_8)
          .retrieve()
          .toEntity(String.class)
          .doOnError(SSLHandshakeException.class, e ->
              fetchFail("SSL handshake fail %s", poolHash))
          .doOnError(SslHandshakeTimeoutException.class, e ->
              fetchFail("SSL handshake time out %s", poolHash))
          .doOnError(ClosedChannelException.class, throwable ->
              fetchFail("Time out %s", poolHash))
          .map(response -> {

            switch (response.getStatusCode()) {

              case NOT_FOUND:
                fetchFail("Not Found ", poolHash);
                return Optional.empty();

              case FORBIDDEN:
                //log.error("FORBIDDEN for url: {}", poolHash.getUrl());
                return Optional.empty();
              case REQUEST_TIMEOUT:
                //log.error("REQUEST_TIMEOUT for url: {}", poolHash.getUrl());
                return Optional.empty();
              case MOVED_PERMANENTLY:
                //log.error("MOVED PERMANENTLY for url: {}", poolHash.getUrl());
                return Optional.empty();
              case OK:
                if (response.getHeaders().get(HttpHeaders.CONTENT_TYPE)
                    .stream()
                    .noneMatch(contentType ->
                        contentType.contains(MediaType.APPLICATION_JSON_VALUE) ||
                            contentType.contains(MediaType.TEXT_PLAIN_VALUE) ||
                            contentType.contains(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    )) {
                  return Optional.empty();
                }

                if (response.getBody().getBytes().length > LIMIT_BYTES) {
                  return Optional.empty();
                }
                return Optional.of(response);

              default:
                log.info("unhandled code {} for url: {}", response.getStatusCode(),poolHash.getUrl());
                return Optional.of(response);
            }
          }).toFuture().thenAccept(
              responseOptional ->
                  responseOptional.ifPresentOrElse(response -> {
                    ResponseEntity responseEntity = (ResponseEntity) response;
                    var responseBody = responseEntity.getBody().toString();

                    PoolData data = PoolData.builder()
                        .status(HttpStatus.OK.value())
                        .json(responseBody.getBytes(StandardCharsets.UTF_8))
                        .poolId(poolHash.getPoolId())
                        .metadataRefId(poolHash.getMetadataId())
                        .hash(
                            HexUtil.encodeHexString(
                                Blake2bUtil.blake2bHash256(
                                    responseBody.getBytes())
                            ))
                        .build();

                    //log.info("Fetch success {} \n {}", poolHash.getUrl(),responseEntity.getBody());
                    applicationEventPublisher.publishEvent(new FetchPoolDataSuccess(data));
                  }, () -> {
                    fetchFail("Response larger than 512 bytes or response body not in json with",
                        poolHash);
                  }));
    } catch (SSLException e) {

    }


  }

  /**
   * Check input string ís URL or not
   *
   * @param string url string
   * @return boolean if url string return true else return false
   */
  private boolean isUrl(String string) {
    String urlRegex = "(http(s)?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)";

    Pattern pattern = Pattern.compile(urlRegex);
    Matcher matcher = pattern.matcher(string);
    return matcher.matches();
  }

  /**
   * Logging and send event for fetching pool data fail
   *
   * @param log      log content
   * @param poolHash
   */
  private void fetchFail(String log, PoolHashUrlProjection poolHash) {
    PoolData data = PoolData.builder()
        .status(HttpStatus.NOT_FOUND.value())
        .errorMessage(String.format(log, poolHash.getUrl()))
        .poolId(poolHash.getPoolId())
        .metadataRefId(poolHash.getMetadataId())
        .build();

    //log.error("{} {}", log, poolHash.getUrl());
    applicationEventPublisher.publishEvent(new FetchPoolDataFail(data));
  }



  @Transactional(rollbackFor = RuntimeException.class)
  public void savePoolData(Map<String, PoolOfflineData> poolOfflineData,
      Map<String, PoolOfflineFetchError> poolOfflineFetchErrors) {
    poolOfflineDataRepository.saveAll(poolOfflineData.values());
    poolOfflineData.clear();

    if (!CollectionUtils.isEmpty(poolOfflineFetchErrors)) {
      poolOfflineFetchErrorRepository.saveAll(poolOfflineFetchErrors.values());
      poolOfflineFetchErrors.clear();
    }
  }

}
