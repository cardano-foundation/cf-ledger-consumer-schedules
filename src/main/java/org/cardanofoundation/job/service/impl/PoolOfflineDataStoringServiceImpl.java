package org.cardanofoundation.job.service.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  public void insertSuccessPoolOfflineData(List<PoolData> successPools) {
    Set<PoolOfflineData> savingPoolData = new HashSet<>();
    var existedPoolData =
        poolOfflineDataRepository.findPoolOfflineDataHashByPoolIds(
            successPools.stream().map(PoolData::getPoolId).toList());

    handleExistedPoolOfflineData(successPools, savingPoolData, existedPoolData);

    poolOfflineDataRepository.saveAll(savingPoolData.stream()
        .sorted(Comparator.comparing(PoolOfflineData::getPoolId)
            .thenComparing(poolOfflineData -> poolOfflineData.getPoolMetadataRef().getId()))
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
      if(!poolMetadataRef.containsKey(poolMetaDataRefId)){
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
   * @param poolData        list pool data need to handle
   * @param savingPoolData  set of Pool Offline Metadata need to save
   * @param existedPoolData set of pool data in database
   */
  private void handleExistedPoolOfflineData(
      List<PoolData> poolData,
      Set<PoolOfflineData> savingPoolData,
      Set<PoolOfflineHashProjection> existedPoolData) {

    poolData.forEach(
        pod -> {
          Optional<PoolOfflineHashProjection> poolOfflineHash =
              findPoolWithPoolRef(existedPoolData, pod);

          poolOfflineHash.ifPresentOrElse(
              exPod -> {
                if (!exPod.getHash()
                    .equals(pod.getHash())) { // update exist pool metadata when if json data change
                  poolOfflineDataRepository
                      .findByPoolIdAndAndPmrId(pod.getPoolId(), pod.getMetadataRefId())
                      .ifPresent(
                          offlineData -> {
                            offlineData.setJson(Arrays.toString(pod.getJson()));
                            offlineData.setBytes(pod.getJson());
                            offlineData.setHash(pod.getHash());

                            savingPoolData.add(offlineData);
                          });
                }
              },
              () -> {
                Optional<PoolOfflineData> existSavingPoolData =
                    savingPoolData.stream()
                        .filter(
                            exPod ->
                                pod.getHash().equals(exPod.getHash())
                                    && pod.getPoolId().equals(exPod.getPoolId()))
                        .findFirst(); // check if handle duplicate data

                if (existSavingPoolData.isEmpty()) {
                  mapPoolOfflineData(pod).ifPresent(savingPoolData::add);
                }
              });
        });
  }

  /**
   * Find pool have offline data of not
   *
   * @param existedPoolData Set of existed Pool Offline Data
   * @param pod             data object transfer of pool offline data {
   * @return if existed Optional of {@link PoolOfflineHashProjection} projection of empty
   */
  private Optional<PoolOfflineHashProjection> findPoolWithPoolRef(
      Set<PoolOfflineHashProjection> existedPoolData, PoolData pod) {
    return existedPoolData.stream()
        .filter(
            exPod ->
                exPod.getPoolRefId().equals(pod.getMetadataRefId()) && pod.getPoolId().equals(exPod.getPoolId()))
        .findFirst();
  }

  /**
   * Extract json of return pool data for mapping entity pool offline data
   *
   * @param poolData data object transfer of pool offline data
   * @return Optional of PoolOfflineData or empty
   */
  private Optional<PoolOfflineData> mapPoolOfflineData(PoolData poolData) {

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

      return buildOfflineData(poolData, map);
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
   * @param poolData data object transfer of pool offline data
   * @param map      map contains features extracted from json
   * @return Optional of PoolOfflineData or empty
   */
  private Optional<PoolOfflineData> buildOfflineData(PoolData poolData, Map<String, Object> map) {

    String name = null;

    var poolHash = poolHashRepository.findById(poolData.getPoolId());
    var poolMetadataRef = poolMetadataRefRepository.findById(poolData.getMetadataRefId());

    if (poolHash.isEmpty() || poolMetadataRef.isEmpty()) {
      return Optional.empty();
    }

    if (ObjectUtils.isEmpty(map.get(POOL_NAME))) {
      name = poolHash.get().getView();
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
            .pool(poolHash.get())
            .poolMetadataRef(poolMetadataRef.get())
            .hash(poolData.getHash())
            .poolName(name)
            .tickerName(String.valueOf(map.get(TICKER)))
            .json(jsonFormatted)
            .bytes(poolData.getJson())
            .build());
  }
}
