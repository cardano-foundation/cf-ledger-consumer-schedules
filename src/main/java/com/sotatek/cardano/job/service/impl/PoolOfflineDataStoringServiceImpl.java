package com.sotatek.cardano.job.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sotatek.cardano.common.entity.PoolOfflineData;
import com.sotatek.cardano.job.constant.JobConstants;
import com.sotatek.cardano.job.dto.PoolData;
import com.sotatek.cardano.job.projection.PoolOfflineHashProjection;
import com.sotatek.cardano.job.repository.PoolHashRepository;
import com.sotatek.cardano.job.repository.PoolMetadataRefRepository;
import com.sotatek.cardano.job.repository.PoolOfflineDataRepository;
import com.sotatek.cardano.job.service.interfaces.PoolOfflineDataStoringService;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class PoolOfflineDataStoringServiceImpl implements PoolOfflineDataStoringService {

  private final PoolMetadataRefRepository poolMetadataRefRepository;
  static final String POOL_NAME = "name";
  static final String TICKER = "ticker";
  final PoolOfflineDataRepository poolOfflineDataRepository;
  final PoolHashRepository poolHashRepository;
  final ObjectMapper objectMapper;

  public PoolOfflineDataStoringServiceImpl(PoolOfflineDataRepository poolOfflineDataRepository,
      PoolHashRepository poolHashRepository,
      ObjectMapper objectMapper,
      PoolMetadataRefRepository poolMetadataRefRepository) {
    this.poolOfflineDataRepository = poolOfflineDataRepository;
    this.poolHashRepository = poolHashRepository;
    this.objectMapper = objectMapper;
    this.poolMetadataRefRepository = poolMetadataRefRepository;
  }

  @Override
  public void insertBatch(Set<PoolData> successPools) {
    log.info("Fetch Data size {}", successPools.size());
    Set<PoolOfflineData> savingPoolData = new HashSet<>();
    var existedPoolData = poolOfflineDataRepository.
        findPoolOfflineDataHashByPoolIds(
            successPools
                .stream()
                .map(PoolData::getPoolId)
                .collect(Collectors.toList()));

    handleExistedPoolOfflineData(successPools, savingPoolData, existedPoolData);

    poolOfflineDataRepository.saveAll(savingPoolData);
    log.info("Insert batch size {}", savingPoolData.size());
    successPools.clear();
  }

  /**
   * @param poolData
   * @param savingPoolData
   * @param existedPoolData
   */
  private void handleExistedPoolOfflineData(Set<PoolData> poolData,
      Set<PoolOfflineData> savingPoolData,
      Set<PoolOfflineHashProjection> existedPoolData) {

    poolData.forEach(pod -> {
      Optional<PoolOfflineHashProjection> poolOfflineHash = findPoolWithSameHash(existedPoolData,
          pod);

      poolOfflineHash.ifPresentOrElse(exPod -> {

            if (!exPod.getHash().equals(pod.getHash())) {
              poolOfflineDataRepository.
                  findByPoolIdAndAndPmrId(pod.getPoolId(), pod.getMetadataRefId())
                  .ifPresent(offlineData -> {
                    offlineData.setJson(Arrays.toString(pod.getJson()));
                    offlineData.setBytes(pod.getJson());
                    offlineData.setHash(pod.getHash());

                    savingPoolData.add(offlineData);
                  });
            }
          }
          , () -> {
            Optional<PoolOfflineData> existSavingPoolData = savingPoolData.stream()
                .filter(exPod -> pod.getHash().equals(exPod.getHash()) && pod.getPoolId()
                    .equals(exPod.getPoolId()))
                .findFirst();

            if (existSavingPoolData.isEmpty()) {
              mapPoolOfflineData(pod).ifPresent(savingPoolData::add);
            }
          }

      );
    });
  }

  /**
   * Find pool have offline data of not
   *
   * @param existedPoolData Set of existed Pool Offline Data
   * @param pod             data object transfer of pool offline data {
   * @return if existed Optional of {@link PoolOfflineHashProjection} projection of empty
   */
  private Optional<PoolOfflineHashProjection> findPoolWithSameHash(
      Set<PoolOfflineHashProjection> existedPoolData, PoolData pod) {
    return existedPoolData.stream()
        .filter(exPod -> exPod.getHash().equals(pod.getHash()) &&
            pod.getPoolId().equals(exPod.getPoolId()))
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
      Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {
      });

      if (CollectionUtils.isEmpty(map)) {
        log.error(
            String.format("pool id %d response data can't convert to json",
                poolData.getPoolId()));
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
    String jsonFormatted = new String(poolData.getJson()).replace(
            String.valueOf(JobConstants.END_LINE), "")
        .replace("\\t+", "")
        .replace("\\s+", "")
        .strip();

    return Optional.of(PoolOfflineData.builder()
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
