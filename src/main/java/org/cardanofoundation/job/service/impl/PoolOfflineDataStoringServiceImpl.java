package org.cardanofoundation.job.service.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.cardanofoundation.explorer.consumercommon.entity.PoolHash;
import org.cardanofoundation.explorer.consumercommon.entity.PoolMetadataRef;
import org.cardanofoundation.explorer.consumercommon.entity.PoolOfflineData;
import org.cardanofoundation.explorer.consumercommon.entity.PoolOfflineFetchError;
import org.cardanofoundation.job.constant.JobConstants;
import org.cardanofoundation.job.dto.PoolData;
import org.cardanofoundation.job.projection.PoolOfflineHashProjection;
import org.cardanofoundation.job.repository.PoolHashRepository;
import org.cardanofoundation.job.repository.PoolMetadataRefRepository;
import org.cardanofoundation.job.repository.PoolOfflineDataRepository;
import org.cardanofoundation.job.repository.PoolOfflineFetchErrorRepository;
import org.cardanofoundation.job.service.PoolOfflineDataStoringService;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class PoolOfflineDataStoringServiceImpl implements PoolOfflineDataStoringService {

  public static final int MAX_RECORDS = 1000;
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
  public void insertSuccessPoolOfflineData(Queue<PoolData> successPools) {
    // key::A Pair of pool id, pool offline data hash
    final ConcurrentHashMap<Pair<Long, String>, PoolOfflineData> savingPoolData = new ConcurrentHashMap<>();
    //key::Long, value::PoolOfflineData
    final Set<PoolOfflineData> poolOfflineData = ConcurrentHashMap.newKeySet();
    //key::Long, value::PoolHash
    final ConcurrentHashMap<Long, PoolHash> poolHashes = new ConcurrentHashMap<>();
    //key::Long, value::PoolMetadataRef
    final ConcurrentHashMap<Long, PoolMetadataRef> poolMetadata = new ConcurrentHashMap<>();
    final int successPoolsPartition = (int) Math.ceil((double) successPools.size() / MAX_RECORDS);

    /*
     * Get list of exist PoolOfflineData and put it and relate entity to map
     * for speed up find process
     */
    Lists.partition(successPools.stream().filter(PoolData::isValid).toList(), successPoolsPartition)
        .parallelStream()
        .forEach(pools -> {
          Set<PoolOfflineData> offlineDataBatch = poolOfflineDataRepository.findPoolOfflineDataHashByPoolMetadataRefIds(
              pools.stream().map(PoolData::getMetadataRefId).toList());
          poolOfflineData.addAll(offlineDataBatch);
        });

    // Get pool metadata  that not existed in pool offline data
    final List<Long> poolMetadataIds = successPools.stream()
        .filter(PoolData::isValid)
        .map(PoolData::getMetadataRefId)
        .filter(poolMetaDataId -> !poolMetadata.containsKey(poolMetaDataId))
        .toList();

    final int poolMetaDataPartition = (int) Math.ceil(
        (double) poolMetadataIds.size() / MAX_RECORDS);

    Lists.partition(poolMetadataIds, poolMetaDataPartition)
        .parallelStream()
        .forEach(splitPoolMetadataIds ->
            poolMetadataRefRepository.findByIdIn(splitPoolMetadataIds)
                .parallelStream()
                .forEach(poolMetadataRef ->
                    poolMetadata.putIfAbsent(poolMetadataRef.getId(), poolMetadataRef)
                ));
    // Get pool hash that not existed in pool offline data
    final List<Long> poolHashIds = successPools.stream()
        .filter(PoolData::isValid)
        .map(PoolData::getPoolId)
        .filter(poolId -> !poolHashes.containsKey(poolId))
        .toList();

    final int poolHashPartition = (int) Math.ceil((double) poolHashIds.size() / MAX_RECORDS);

    Lists.partition(poolHashIds, poolHashPartition)
        .parallelStream()
        .forEach(splitPoolMetadataIds ->
            poolHashRepository.findByIdIn(splitPoolMetadataIds)
                .parallelStream()
                .forEach(poolHash -> poolHashes.putIfAbsent(poolHash.getId(), poolHash))
        );

    successPools.parallelStream()
        .filter(PoolData::isValid)
        .forEach(poolData ->
            findPoolWithPoolRef(poolOfflineData, poolData)
                .ifPresentOrElse(
                    existedPod -> { // if  pool offline data exist check hash change
                      // update exist pool metadata when if json data change
                      if (!existedPod.getHash().equals(poolData.getHash())) {
                        existedPod.setJson(Arrays.toString(poolData.getJson()));
                        existedPod.setBytes(poolData.getJson());
                        existedPod.setHash(poolData.getHash());
                        existedPod.setLogoUrl(poolData.getLogoUrl());
                        existedPod.setIconUrl(poolData.getIconUrl());
                        savingPoolData.put(Pair.of(existedPod.getPoolId(),
                            existedPod.getHash()), existedPod);
                      } else { // this
                        if (ObjectUtils.isEmpty(existedPod.getIconUrl()) &&
                            ObjectUtils.isEmpty(existedPod.getLogoUrl())) {
                          existedPod.setLogoUrl(
                              !ObjectUtils.isEmpty(poolData.getLogoUrl()) ? poolData.getIconUrl()
                                                                          : poolData.getLogoUrl());
                          existedPod.setIconUrl(
                              !ObjectUtils.isEmpty(poolData.getIconUrl()) ? poolData.getIconUrl()
                                                                          : poolData.getLogoUrl());
                        }
                      }
                    }, () -> {
                      final var offlineDataKey = Pair.of(poolData.getPoolId(),
                          poolData.getHash());
                      Optional<PoolOfflineData> existSavingPoolData =
                          Optional.ofNullable(
                              savingPoolData.get(offlineDataKey)); // check if handle duplicate data

                      if (existSavingPoolData.isPresent()) {
                        final var onSavingOfflineData = existSavingPoolData.get();
                        if (onSavingOfflineData.getPmrId() > poolData.getMetadataRefId()) {
                          return;
                        }
                      }

                      mapPoolOfflineData(poolData, poolHashes, poolMetadata)
                          .ifPresent(offlineData ->
                              savingPoolData.put(offlineDataKey, offlineData));

                    }));

    poolOfflineDataRepository.saveAll(savingPoolData.values().stream()
        .sorted(Comparator.comparing(PoolOfflineData::getPoolId)
            .thenComparing(offlineData -> offlineData.getPoolMetadataRef().getId()))
        .toList());
    log.info("Insert batch size {}", savingPoolData.size());
  }


  @Override
  public void insertFailOfflineData(List<PoolData> failedPools) {

    // key: PoolOfflineFetchError::getId, value: PoolOfflineFetchError
    Map<Long, PoolOfflineFetchError> failPoolOfflineData = poolOfflineFetchErrorRepository.findPoolOfflineFetchErrorByPoolMetadataRefIn(
            failedPools.stream()
                .map(PoolData::getMetadataRefId)
                .toList())
        .stream()
        .collect(
            Collectors.toMap(
                poolOfflineFetchError -> poolOfflineFetchError.getPoolMetadataRef().getId(),
                Function.identity()));

    List<Long> usedPoolId = failPoolOfflineData.values()
        .stream()
        .map(poolOfflineFetchError -> poolOfflineFetchError.getPoolHash().getId())
        .toList();

    List<Long> usedPoolMetadataRef = failPoolOfflineData.keySet().stream().toList();

    Map<Long, PoolHash> poolHashMap = poolHashRepository.findByIdIn(
            failedPools.stream().map(PoolData::getPoolId)
                .filter(poolId -> !usedPoolId.contains(poolId))
                .toList())
        .stream()
        .collect(Collectors.toMap(PoolHash::getId, Function.identity()));

    Map<Long, PoolMetadataRef> poolMetadataRef = poolMetadataRefRepository.findByIdIn(
            failedPools.stream()
                .map(PoolData::getMetadataRefId)
                .filter(failedMetaData -> !usedPoolMetadataRef.contains(failedMetaData))
                .toList())
        .stream()
        .collect(Collectors.toMap(PoolMetadataRef::getId, Function.identity()));

    failPoolOfflineData.values().forEach(poolOfflineFetchError -> {
      final Long poolId = poolOfflineFetchError.getPoolHash().getId();
      if (!poolHashMap.containsKey(poolId)) {
        poolHashMap.put(poolId, poolOfflineFetchError.getPoolHash());
      }

      final Long poolMetaDataRefId = poolOfflineFetchError.getPoolMetadataRef().getId();
      if (!poolMetadataRef.containsKey(poolMetaDataRefId)) {
        poolMetadataRef.put(poolMetaDataRefId, poolOfflineFetchError.getPoolMetadataRef());
      }
    });

    failedPools
        .parallelStream()
        .forEach(failData ->
            Optional.ofNullable(failPoolOfflineData.get(failData.getMetadataRefId()))
                .ifPresentOrElse(poolOfflineFetchError -> {
                  poolOfflineFetchError.setFetchTime(
                      Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)));
                  poolOfflineFetchError.setRetryCount(
                      poolOfflineFetchError.getRetryCount() + BigInteger.ONE.intValue());
                }, () -> {
                  PoolOfflineFetchError error = PoolOfflineFetchError.builder()
                      .poolHash(poolHashMap.get(failData.getPoolId()))
                      .poolMetadataRef(poolMetadataRef.get(failData.getMetadataRefId()))
                      .fetchError(failData.getErrorMessage())
                      .retryCount(BigInteger.ONE.intValue())
                      .fetchTime(Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)))
                      .build();

                  failPoolOfflineData.put(failData.getMetadataRefId(), error);
                }));
    poolOfflineFetchErrorRepository.saveAll(failPoolOfflineData.values());
  }


  /**
   * Find pool have offline data of not
   *
   * @param existedPoolData Set of existed Pool Offline Data
   * @param pod             data object transfer of pool offline data {
   * @return if existed Optional of {@link PoolOfflineHashProjection} projection of empty
   */
  private Optional<PoolOfflineData> findPoolWithPoolRef(
      Set<PoolOfflineData> existedPoolData, PoolData pod) {
    return existedPoolData.stream()
        .filter(
            exPod ->
                exPod.getPmrId().equals(pod.getMetadataRefId()) && pod.getPoolId()
                    .equals(exPod.getPoolId()))
        .findFirst();
  }

  /**
   * Extract json of return pool data for mapping entity pool offline data
   *
   * @param poolData     data object transfer of pool offline data
   * @param poolHashes
   * @param poolMetadata
   * @return Optional of PoolOfflineData or empty
   */
  private Optional<PoolOfflineData> mapPoolOfflineData(PoolData poolData,
                                                       Map<Long, PoolHash> poolHashes,
                                                       Map<Long, PoolMetadataRef> poolMetadata) {

    if (ObjectUtils.isEmpty(poolData.getJson())) {
      return Optional.empty();
    }

    String json = new String(poolData.getJson());
    try {
      Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});

      if (CollectionUtils.isEmpty(map)) {
        log.error(
            String.format("pool id %d response data can't convert to json", poolData.getPoolId()));
        return Optional.empty();
      }

      return buildOfflineData(poolData, map, poolHashes, poolMetadata);
    } catch (JsonMappingException e) {
      log.error("can't map {}", json);
    } catch (IOException e) {
      log.error("can't deserializer {} {}", poolData.getMetadataRefId(), json);
    }
    return Optional.empty();
  }

  /**
   * Mapping pool offline data features input parameters
   *
   * @param poolData     data object transfer of pool offline data
   * @param map          map contains features extracted from json
   * @param poolHashes
   * @param poolMetadata
   * @return Optional of PoolOfflineData or empty
   */
  private Optional<PoolOfflineData> buildOfflineData(PoolData poolData, Map<String, Object> map,
                                                     Map<Long, PoolHash> poolHashes,
                                                     Map<Long, PoolMetadataRef> poolMetadata) {
    String name;

    var poolHash = poolHashes.get(poolData.getPoolId());
    var poolMetadataRef = poolMetadata.get(poolData.getMetadataRefId());

    if (ObjectUtils.isEmpty(map.get(POOL_NAME))) {
      name = poolHash.getView();
    } else {
      name = String.valueOf(map.get(POOL_NAME));
    }
    String jsonFormatted =
        new String(poolData.getJson())
            .replace(String.valueOf(JobConstants.END_LINE), "")
            .replace("\\t+", "")
            .replace("\\s+", "")
            .strip();

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
                !ObjectUtils.isEmpty(poolData.getLogoUrl()) ? poolData.getIconUrl()
                                                            : poolData.getLogoUrl())
            .iconUrl(
                !ObjectUtils.isEmpty(poolData.getIconUrl()) ? poolData.getIconUrl()
                                                            : poolData.getLogoUrl())
            .build());
  }
}
