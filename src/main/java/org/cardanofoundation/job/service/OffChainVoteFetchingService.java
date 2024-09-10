package org.cardanofoundation.job.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.apache.logging.log4j.util.Strings;
import reactor.netty.http.client.HttpClient;

import org.cardanofoundation.explorer.common.utils.UrlUtil;
import org.cardanofoundation.job.dto.govActionMetaData.Anchor;
import org.cardanofoundation.job.dto.govActionMetaData.OffChainFetchResult;

@Log4j2
public abstract class OffChainVoteFetchingService<S, F> {

  @Value("${application.api.ipfs.base-url}")
  private String ipfsGatewayBaseUrl;

  static final int TIMEOUT = 30000;
  static final int READ_TIMEOUT = 19000;
  static final int WRITE_TIMEOUT = 10000;
  static final int LIMIT_BYTES = 4096;

  protected Queue<OffChainFetchResult> offChainAnchorsFetchResult;

  public OffChainVoteFetchingService() {
    initOffChainListData();
  }

  public void initOffChainListData() {
    offChainAnchorsFetchResult = new ConcurrentLinkedQueue<>();
  }

  public abstract S extractRawOffChainData(OffChainFetchResult offChainAnchorData);

  public abstract F extractFetchError(OffChainFetchResult offChainAnchorData);

  public List<S> getOffChainAnchorsFetchSuccess() {
    return offChainAnchorsFetchResult.stream()
        .filter(OffChainFetchResult::isFetchSuccess)
        .map(this::extractRawOffChainData)
        .toList();
  }

  public List<F> getOffChainAnchorsFetchError() {
    return offChainAnchorsFetchResult.stream()
        .filter(offChainFetchResult -> !offChainFetchResult.isFetchSuccess())
        .map(this::extractFetchError)
        .toList();
  }

  public void crawlOffChainAnchors(Collection<Anchor> anchors) {
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    Set<Anchor> anchorSet = new HashSet<>(anchors);
    anchorSet.forEach(anchor -> futures.add(fetchAnchorUrl(anchor)));
    futures.forEach(CompletableFuture::join);
  }

  protected CompletableFuture<Void> fetchAnchorUrl(Anchor anchor) {
    try {
      String anchorUrl = anchor.getAnchorUrl();
      if (!UrlUtil.isUrl(anchorUrl) && !isIPFSUrl(anchorUrl)) {
        handleFetchFailure("Invalid URL", anchor);
        return CompletableFuture.completedFuture(null);
      } else if (!UrlUtil.isUrl(anchorUrl) && isIPFSUrl(anchorUrl)) {
        anchorUrl = getIPFSUrl(anchorUrl);
      }
      return buildWebClient()
          .get()
          .uri(UrlUtil.formatSpecialCharactersUrl(anchorUrl))
          .acceptCharset(StandardCharsets.UTF_8)
          .retrieve()
          .toEntity(String.class)
          .timeout(Duration.ofMillis(TIMEOUT))
          .doOnError(
              Exception.class, throwable -> handleFetchFailure(throwable.getMessage(), anchor))
          .toFuture()
          .thenAccept(responseEntity -> handleResponse(responseEntity, anchor))
          .exceptionally(
              throwable -> {
                log.info("Error when fetch data from URL: {}", throwable.getMessage());
                handleFetchFailure(throwable.getMessage(), anchor);
                return null;
              });

    } catch (Exception e) {
      handleFetchFailure(e.getMessage(), anchor);
      return CompletableFuture.completedFuture(null);
    }
  }

  private void handleFetchFailure(String error, Anchor anchor) {
    OffChainFetchResult data =
        OffChainFetchResult.builder()
            .anchorHash(anchor.getAnchorHash())
            .anchorUrl(anchor.getAnchorUrl())
            .isFetchSuccess(false)
            .fetchFailError(error)
            .build();

    offChainAnchorsFetchResult.add(data);
  }

  private void handleFetchSuccess(Anchor anchor, String responseBody) {
    OffChainFetchResult data =
        OffChainFetchResult.builder()
            .anchorHash(anchor.getAnchorHash())
            .anchorUrl(anchor.getAnchorUrl())
            .isFetchSuccess(true)
            .fetchFailError(null)
            .rawData(responseBody)
            .build();

    offChainAnchorsFetchResult.add(data);
  }

  private void handleResponse(ResponseEntity<String> response, Anchor anchor) {
    HttpStatusCode statusCode = response.getStatusCode();

    // if status code is not OK, then send to fail queue
    if (!HttpStatus.OK.equals(statusCode)) {
      handleFetchFailure(statusCode.toString(), anchor);
      return;
    }

    // if content type is not supported, then send to fail queue
    if (Objects.requireNonNull(response.getHeaders().get(HttpHeaders.CONTENT_TYPE)).stream()
        .noneMatch(
            contentType ->
                contentType.contains(MediaType.APPLICATION_JSON_VALUE)
                    || contentType.contains("application/ld+json")
                    || contentType.contains(MediaType.TEXT_PLAIN_VALUE)
                    || contentType.contains("text/json"))) {
      handleFetchFailure(
          "Content type not supported " + response.getHeaders().get(HttpHeaders.CONTENT_TYPE),
          anchor);
      return;
    }

    // if content length is greater than limit, then send to fail queue
    if (Objects.requireNonNull(response.getBody()).getBytes().length > LIMIT_BYTES) {
      handleFetchFailure("Content length exceed limit", anchor);
      return;
    }

    var responseBody = response.getBody();
    if (Objects.nonNull(responseBody)) {
      handleFetchSuccess(anchor, responseBody);
    }
  }

  /**
   * Constructs and configures a WebClient with specific settings for handling HTTP requests,
   * including SSL configuration, timeout settings, and headers.
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

    return WebClient.builder()
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }

  private Boolean isIPFSUrl(String url) {
    if (Objects.isNull(url)) {
      return Boolean.FALSE;
    }
    return url.startsWith("ipfs://");
  }

  private String getIPFSUrl(String url) {
    if (Objects.isNull(url)) {
      return null;
    }
    String cid = url.substring(7);
    if (Strings.isBlank(cid)) {
      return null;
    }
    return Strings.concat(ipfsGatewayBaseUrl, cid);
  }
}
