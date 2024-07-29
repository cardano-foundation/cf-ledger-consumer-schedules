package org.cardanofoundation.job.service.impl;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import javax.net.ssl.SSLException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

import org.cardanofoundation.explorer.common.entity.enumeration.DataCheckpointType;
import org.cardanofoundation.explorer.common.entity.explorer.DataCheckpoint;
import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteFetchError;
import org.cardanofoundation.explorer.common.utils.HexUtil;
import org.cardanofoundation.explorer.common.utils.UrlUtil;
import org.cardanofoundation.job.dto.govActionMetaData.Anchor;
import org.cardanofoundation.job.dto.govActionMetaData.GovActionMetaData;
import org.cardanofoundation.job.dto.govActionMetaData.OffChainGovActionData;
import org.cardanofoundation.job.mapper.AnchorMapper;
import org.cardanofoundation.job.projection.gov.AnchorProjection;
import org.cardanofoundation.job.repository.explorer.DataCheckpointRepository;
import org.cardanofoundation.job.repository.ledgersync.GovActionProposalRepository;
import org.cardanofoundation.job.repository.ledgersync.OffChainVoteFetchErrorRepository;
import org.cardanofoundation.job.service.OffChainVoteGovActionDataFetchingService;
import org.cardanofoundation.job.service.OffChainVoteGovActionDataStoringService;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OffChainVoteGovActionFetchingDataServiceImpl
    implements OffChainVoteGovActionDataFetchingService {

  static final int TIMEOUT = 30000;
  static final int READ_TIMEOUT = 19000;
  static final int WRITE_TIMEOUT = 10000;
  static final int LIMIT_BYTES = 4096;
  private final WebClient.Builder webClientBuilder;
  private final GovActionProposalRepository govActionProposalRepository;
  private final OffChainVoteFetchErrorRepository offChainVoteFetchErrorRepository;
  private final DataCheckpointRepository dataCheckpointRepository;
  private final AnchorMapper anchorMapper;
  private ObjectMapper mapper = new ObjectMapper();
  private final OffChainVoteGovActionDataStoringService offChainVoteGovActionDataStoringService;

  @Value("${jobs.gov-action-metadata.retry-count}")
  private Integer retryCount;

  Queue<OffChainGovActionData> offChainGovActionDataList = new ConcurrentLinkedQueue<>();
  Queue<OffChainGovActionData> offChainGovActionDataListRetry = new ConcurrentLinkedQueue<>();

  @PostConstruct
  void setup() {
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  @Transactional
  public Queue<OffChainGovActionData> getDataFromAnchorUrl() {
    Timestamp lastFetchTime = new Timestamp(System.currentTimeMillis());

    Long maxSlotNo = govActionProposalRepository.maxSlotNo().orElse(null);

    DataCheckpoint currentCheckpoint =
        dataCheckpointRepository.findFirstByType(DataCheckpointType.GOV_ACTION_DATA).orElse(null);
    if (currentCheckpoint == null) {
      currentCheckpoint =
          DataCheckpoint.builder()
              .type(DataCheckpointType.GOV_ACTION_DATA)
              .updateTime(lastFetchTime)
              .build();
      firstTimeInitialization(maxSlotNo);
    } else {
      updateFromExistingCheckpoint(currentCheckpoint.getSlotNo(), maxSlotNo);
      currentCheckpoint.setUpdateTime(lastFetchTime);
    }
    // ensure that rollback mechanism is working
    currentCheckpoint.setSlotNo(maxSlotNo - 43200);
    dataCheckpointRepository.save(currentCheckpoint);
    return offChainGovActionDataList;
  }

  @Override
  public void retryFetchGovActionMetadata() {
    List<OffChainVoteFetchError> fetchErrors =
        offChainVoteFetchErrorRepository.findByRetryCountLessThanEqual(retryCount);
    List<Anchor> anchors =
        fetchErrors.stream()
            .map(
                projection -> {
                  Anchor anchor = new Anchor();
                  anchor.setAnchorUrl(projection.getAnchorUrl());
                  anchor.setAnchorHash(projection.getAnchorHash());
                  return anchor;
                })
            .toList();
    createFetchRequest(anchors, true).forEach(CompletableFuture::join);
    offChainVoteGovActionDataStoringService.insertFetchRetryData(
        fetchErrors, offChainGovActionDataListRetry);
  }

  private List<CompletableFuture<Void>> createFetchRequest(List<Anchor> anchors, Boolean isRetry) {
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    Set<Anchor> anchorSet = new HashSet<>(anchors);
    anchorSet.forEach(projection -> futures.add(fetchAnchorUrl(projection, isRetry)));
    return futures;
  }

  private void firstTimeInitialization(Long maxSlotNo) {
    log.info("First time initialization for fetching data from anchor URL");
    Pageable pageable = PageRequest.of(0, 10);
    Slice<AnchorProjection> anchorProjections =
        govActionProposalRepository.getAnchorUrlAndHash(pageable, 0L, maxSlotNo);
    List<Anchor> anchors =
        new ArrayList<>(
            anchorProjections.getContent().stream()
                .map(anchorMapper::fromAnchorProjection)
                .toList());
    List<CompletableFuture<Void>> futures = new ArrayList<>(createFetchRequest(anchors, false));
    while (anchorProjections.hasNext()) {
      anchorProjections =
          govActionProposalRepository.getAnchorUrlAndHash(
              anchorProjections.nextPageable(), 0L, maxSlotNo);
      anchors.clear();
      anchors.addAll(
          anchorProjections.getContent().stream().map(anchorMapper::fromAnchorProjection).toList());
      futures.addAll(createFetchRequest(anchors, false));
    }
    futures.forEach(CompletableFuture::join);
  }

  private void updateFromExistingCheckpoint(Long currentCheckpoint, Long maxSlotNo) {
    log.info("Update from existing checkpoint for fetching data from anchor URL");
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    Pageable pageable = PageRequest.of(0, 10);
    Slice<AnchorProjection> anchorProjections =
        govActionProposalRepository.getAnchorUrlAndHash(pageable, currentCheckpoint, maxSlotNo);

    List<Anchor> anchors =
        anchorProjections.getContent().stream().map(anchorMapper::fromAnchorProjection).toList();
    futures.addAll(createFetchRequest(anchors, false));
    while (anchorProjections.hasNext()) {
      anchorProjections =
          govActionProposalRepository.getAnchorUrlAndHash(
              anchorProjections.nextPageable(), currentCheckpoint, maxSlotNo);
      anchors.clear();
      anchors.addAll(
          anchorProjections.getContent().stream().map(anchorMapper::fromAnchorProjection).toList());
      futures.addAll(createFetchRequest(anchors, false));
    }
    futures.forEach(CompletableFuture::join);
  }

  private CompletableFuture<Void> fetchAnchorUrl(Anchor anchor, Boolean isRetry) {
    try {
      if (!UrlUtil.isUrl(anchor.getAnchorUrl())) {
        fetchFail("Invalid URL", anchor, isRetry);
        return CompletableFuture.completedFuture(null);
      }
      return buildWebClient()
          .get()
          .uri(UrlUtil.formatSpecialCharactersUrl(anchor.getAnchorUrl()))
          .acceptCharset(StandardCharsets.UTF_8)
          .retrieve()
          .toEntity(String.class)
          .timeout(Duration.ofMillis(TIMEOUT))
          .doOnError(
              Exception.class, throwable -> fetchFail(throwable.getMessage(), anchor, isRetry))
          .toFuture()
          .thenAccept(responseEntity -> handleResponse(responseEntity, anchor, isRetry))
          .exceptionally(
              throwable -> {
                log.info("Error when fetch data from URL: {}", throwable.getMessage());
                //                fetchFail(throwable.getMessage(), anchor, isRetry);
                return null;
              });

    } catch (Exception e) {
      fetchFail(e.getMessage(), anchor, isRetry);
      return CompletableFuture.completedFuture(null);
    }
  }

  private void fetchFail(String error, Anchor anchor, Boolean isRetry) {
    OffChainGovActionData data =
        OffChainGovActionData.builder()
            .anchorHash(anchor.getAnchorHash())
            .anchorUrl(anchor.getAnchorUrl())
            .isFetchSuccess(false)
            .fetchFailReason(error)
            .build();

    if (isRetry) {
      offChainGovActionDataListRetry.add(data);
    } else {
      offChainGovActionDataList.add(data);
    }
  }

  private void fetchSuccess(Anchor anchor, String responseBody, Boolean isRetry) {
    OffChainGovActionData data =
        OffChainGovActionData.builder()
            .anchorHash(anchor.getAnchorHash())
            .anchorUrl(anchor.getAnchorUrl())
            .isFetchSuccess(true)
            .fetchFailReason(null)
            .rawData(responseBody)
            .build();

    getDataFromRawDataAndCheckHashValid(data);
    if (isRetry) {
      offChainGovActionDataListRetry.add(data);
    } else {
      offChainGovActionDataList.add(data);
    }
  }

  private void handleResponse(ResponseEntity<String> response, Anchor anchor, Boolean isRetry) {
    HttpStatusCode statusCode = response.getStatusCode();

    // if status code is not OK, then send to fail queue
    if (!HttpStatus.OK.equals(statusCode)) {
      fetchFail(statusCode.toString(), anchor, isRetry);
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
      fetchFail(
          "Content type not supported " + response.getHeaders().get(HttpHeaders.CONTENT_TYPE),
          anchor,
          isRetry);
      return;
    }

    // if content length is greater than limit, then send to fail queue
    if (Objects.requireNonNull(response.getBody()).getBytes().length > LIMIT_BYTES) {
      fetchFail("Content length exceed limit", anchor, isRetry);
      return;
    }

    var responseBody = response.getBody();
    if (Objects.nonNull(responseBody)) {
      fetchSuccess(anchor, responseBody, isRetry);
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

    return webClientBuilder
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }

  public void getDataFromRawDataAndCheckHashValid(OffChainGovActionData offChainGovActionData) {
    String hash =
        HexUtil.encodeHexString(
            Blake2bUtil.blake2bHash256(offChainGovActionData.getRawData().getBytes()));
    if (!hash.equals(offChainGovActionData.getAnchorHash())) {
      offChainGovActionData.setValid(false);
      return;
    } else {
      offChainGovActionData.setValid(true);
    }
    try {
      GovActionMetaData govActionMetaData =
          mapper.readValue(offChainGovActionData.getRawData(), GovActionMetaData.class);
      if (govActionMetaData.getBody() != null) {
        offChainGovActionData.setTitle(govActionMetaData.getBody().getTitle());
        offChainGovActionData.setAbstractContent(govActionMetaData.getBody().getAbstractContent());
        offChainGovActionData.setMotivation(govActionMetaData.getBody().getMotivation());
        offChainGovActionData.setRationale(govActionMetaData.getBody().getRationale());
      }
    } catch (JsonProcessingException e) {
      log.info("Error when parse data to object from URL: {}", e.getMessage());
    }
  }
}
