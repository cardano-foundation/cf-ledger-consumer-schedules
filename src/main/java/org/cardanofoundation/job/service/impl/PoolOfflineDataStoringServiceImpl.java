package org.cardanofoundation.job.service.impl;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.cardanofoundation.explorer.consumercommon.entity.PoolHash;
import org.cardanofoundation.explorer.consumercommon.entity.PoolMetadataRef;
import org.cardanofoundation.explorer.consumercommon.entity.PoolOfflineData;
import org.cardanofoundation.explorer.consumercommon.entity.PoolOfflineFetchError;
import org.cardanofoundation.job.dto.PoolData;
import org.cardanofoundation.job.repository.ledgersync.PoolHashRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolMetadataRefRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolOfflineDataRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolOfflineFetchErrorRepository;
import org.cardanofoundation.job.service.PoolOfflineDataStoringService;
import org.cardanofoundation.ledgersync.common.util.JsonUtil;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class PoolOfflineDataStoringServiceImpl implements PoolOfflineDataStoringService {

  final PoolOfflineFetchErrorRepository poolOfflineFetchErrorRepository;
  private final PoolMetadataRefRepository poolMetadataRefRepository;
  final PoolOfflineDataRepository poolOfflineDataRepository;
  final PoolHashRepository poolHashRepository;

  final ObjectMapper objectMapper;
  static final String POOL_NAME = "name";
  static final String TICKER = "ticker";

  public PoolOfflineDataStoringServiceImpl(
      PoolOfflineFetchErrorRepository poolOfflineFetchErrorRepository,
      PoolOfflineDataRepository poolOfflineDataRepository,
      PoolHashRepository poolHashRepository,
      ObjectMapper objectMapper,
      PoolMetadataRefRepository poolMetadataRefRepository) {
    this.poolOfflineFetchErrorRepository = poolOfflineFetchErrorRepository;
    this.poolOfflineDataRepository = poolOfflineDataRepository;
    this.poolHashRepository = poolHashRepository;
    this.objectMapper = objectMapper;
    this.poolMetadataRefRepository = poolMetadataRefRepository;
  }

  @Override
  @Transactional
  public void saveSuccessPoolOfflineData(List<PoolData> successPools) {
    long startTime = System.currentTimeMillis();
    log.info("Start saving success pool offline data");
    List<PoolOfflineData> poolOfflineDataNeedSave = new ArrayList<>();

    List<Long> poolIds = successPools.stream().map(PoolData::getPoolId).toList();
    List<Long> poolMetadataRefIds = successPools.stream().map(PoolData::getMetadataRefId).toList();

    Map<Long, PoolData> poolDataMap =
        successPools.stream().collect(Collectors.toMap(PoolData::getPoolId, Function.identity()));
    // pool hash map {key, value} = {poolId, PoolHash}
    Map<Long, PoolHash> poolHashMap =
        poolHashRepository.findByIdIn(poolIds).stream()
            .collect(Collectors.toMap(PoolHash::getId, Function.identity()));
    // pool metadata ref map {key, value} = {poolMetadataRefId, PoolMetadataRef}
    Map<Long, PoolMetadataRef> poolMetadataRefMap =
        poolMetadataRefRepository.findByIdIn(poolMetadataRefIds).stream()
            .collect(Collectors.toMap(PoolMetadataRef::getId, Function.identity()));
    // pool offline data map existed {key, value} = {poolId, PoolOfflineData}
    Map<Long, PoolOfflineData> poolOfflineDataSourceMap =
        poolOfflineDataRepository.findPoolOfflineDataByPoolIdIn(poolIds).stream()
            .collect(Collectors.toMap(PoolOfflineData::getPoolId, Function.identity()));

    poolDataMap.entrySet().parallelStream()
        .forEach(
            poolDataEntry -> {
              Long poolId = poolDataEntry.getKey();
              PoolData poolData = poolDataEntry.getValue();
              PoolHash poolHash = poolHashMap.get(poolId);
              PoolMetadataRef poolMetadataRef = poolMetadataRefMap.get(poolData.getMetadataRefId());
              PoolOfflineData poolOfflineData =
                  buildPoolOfflineData(poolData, poolHash, poolMetadataRef).orElse(null);
              if (Objects.nonNull(poolOfflineData)) {
                // if pool offline data exist then update
                if (poolOfflineDataSourceMap.containsKey(poolId)) {
                  poolOfflineData.setId(poolOfflineDataSourceMap.get(poolId).getId());
                }
                poolOfflineDataNeedSave.add(poolOfflineData);
              }
            });

    poolOfflineDataRepository.saveAll(poolOfflineDataNeedSave);
    log.info(
        "Saved success pool offline data, count: {}, time taken: {} ms",
        poolOfflineDataNeedSave.size(),
        System.currentTimeMillis() - startTime);
  }

  @Override
  @Transactional
  public void saveFailOfflineData(List<PoolData> failedPools) {
    long startTime = System.currentTimeMillis();

    log.info("Start saving fail pool offline data");
    List<PoolOfflineFetchError> poolOfflineFetchErrorsNeedSave = new ArrayList<>();

    List<Long> poolIds = failedPools.stream().map(PoolData::getPoolId).toList();
    List<Long> poolMetadataRefIds = failedPools.stream().map(PoolData::getMetadataRefId).toList();

    // raw pool offline data map {key, value} = {poolId, PoolData}
    Map<Long, PoolData> poolDataMap =
        failedPools.stream()
            .collect(Collectors.toMap(PoolData::getPoolId, Function.identity(), (a, b) -> a));
    // pool hash map {key, value} = {poolId, PoolHash}
    Map<Long, PoolHash> poolHashMap =
        poolHashRepository.findByIdIn(poolIds).stream()
            .collect(Collectors.toMap(PoolHash::getId, Function.identity()));
    // pool metadata ref map {key, value} = {poolMetadataRefId, PoolMetadataRef}
    Map<Long, PoolMetadataRef> poolMetadataRefMap =
        poolMetadataRefRepository.findByIdIn(poolMetadataRefIds).stream()
            .collect(Collectors.toMap(PoolMetadataRef::getId, Function.identity()));

    poolDataMap.entrySet().parallelStream()
        .forEach(
            poolDataEntry -> {
              Long poolId = poolDataEntry.getKey();
              PoolData poolData = poolDataEntry.getValue();
              PoolHash poolHash = poolHashMap.get(poolId);
              PoolMetadataRef poolMetadataRef = poolMetadataRefMap.get(poolData.getMetadataRefId());
              // if pool offline data exist then update
              PoolOfflineFetchError poolOfflineFetchError =
                  poolOfflineFetchErrorRepository.findByPoolHashAndPoolMetadataRef(
                      poolHash, poolMetadataRef);

              // if pool offline fetch error exist then update
              if (!Objects.isNull(poolOfflineFetchError)) {
                poolOfflineFetchError.setFetchTime(
                    Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)));
                poolOfflineFetchError.setRetryCount(poolOfflineFetchError.getRetryCount() + 1);
                poolOfflineFetchError.setFetchError(poolData.getErrorMessage());
              } else {
                poolOfflineFetchError =
                    PoolOfflineFetchError.builder()
                        .poolHash(poolHash)
                        .poolMetadataRef(poolMetadataRef)
                        .fetchError(poolData.getErrorMessage())
                        .retryCount(BigInteger.ONE.intValue())
                        .fetchTime(Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)))
                        .build();
              }
              poolOfflineFetchErrorsNeedSave.add(poolOfflineFetchError);
            });

    poolOfflineFetchErrorRepository.saveAll(poolOfflineFetchErrorsNeedSave);
    log.info(
        "Saved success fail pool offline data, count: {}, time taken: {} ms",
        poolOfflineFetchErrorsNeedSave.size(),
        System.currentTimeMillis() - startTime);
  }

  /**
   * Constructs a PoolOfflineData object by extracting relevant information from the provided
   * PoolData, PoolHash, and PoolMetadataRef objects.
   *
   * @param poolData poolData
   * @param poolHash poolHash
   * @param poolMetadataRef poolMetadataRef
   * @return Optional<PoolOfflineData>
   */
  private Optional<PoolOfflineData> buildPoolOfflineData(
      PoolData poolData, PoolHash poolHash, PoolMetadataRef poolMetadataRef) {
    try {
      String json = new String(poolData.getJson());
      Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
      String name;
      if (ObjectUtils.isEmpty(map.get(POOL_NAME))) {
        name = poolHash.getView();
      } else {
        name = String.valueOf(map.get(POOL_NAME));
      }
      String jsonFormatted = JsonUtil.getPrettyJson(json);
      return Optional.of(
          PoolOfflineData.builder()
              .poolId(poolData.getPoolId())
              .pmrId(poolData.getMetadataRefId())
              .pool(poolHash)
              .poolMetadataRef(poolMetadataRef)
              .hash(poolData.getHash())
              .poolName(name)
              .tickerName(String.valueOf(map.get(TICKER)))
              .json(jsonFormatted)
              .bytes(poolData.getJson())
              .logoUrl(
                  !ObjectUtils.isEmpty(poolData.getLogoUrl())
                      ? poolData.getLogoUrl()
                      : poolData.getIconUrl())
              .iconUrl(
                  !ObjectUtils.isEmpty(poolData.getIconUrl())
                      ? poolData.getIconUrl()
                      : poolData.getLogoUrl())
              .build());
    } catch (JsonProcessingException e) {
      return Optional.empty();
    }
  }
}
