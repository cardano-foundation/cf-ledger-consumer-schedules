package com.sotatek.cardano.job.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sotatek.cardano.common.entity.PoolHash;
import com.sotatek.cardano.common.entity.PoolOfflineData;
import com.sotatek.cardano.job.dto.IdleInsert;
import com.sotatek.cardano.job.dto.PoolData;
import com.sotatek.cardano.job.event.FetchPoolDataSuccess;
import com.sotatek.cardano.job.projection.PoolOfflineHashProjection;
import com.sotatek.cardano.job.repository.PoolHashRepository;
import com.sotatek.cardano.job.repository.PoolMetadataRefRepository;
import com.sotatek.cardano.job.repository.PoolOfflineDataRepository;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class PoolOfflineDataService {

  private final PoolMetadataRefRepository poolMetadataRefRepository;

  static final String POOL_NAME = "name";
  static final String TICKER = "ticker";
  final PoolOfflineDataRepository poolOfflineDataRepository;
  final PoolHashRepository poolHashRepository;
  final ObjectMapper objectMapper;
  final Set<PoolData> successPools;
  @Value("${jobs.install-batch}")
  private int batchSize;
  private IdleInsert idleInsert;

  public PoolOfflineDataService(PoolOfflineDataRepository poolOfflineDataRepository,
      PoolHashRepository poolHashRepository,
      ObjectMapper objectMapper,
      PoolMetadataRefRepository poolMetadataRefRepository) {
    this.poolOfflineDataRepository = poolOfflineDataRepository;
    this.poolHashRepository = poolHashRepository;
    this.objectMapper = objectMapper;
    this.successPools = Collections.synchronizedSet(new HashSet<>());
    idleInsert = IdleInsert.builder().build();
    this.poolMetadataRefRepository = poolMetadataRefRepository;
  }

  @Transactional
  @Scheduled(fixedDelayString = "${jobs.insert-pool-offline-data.delay}",
      initialDelayString = "${jobs.insert-pool-offline-data.innit}")
  public void updatePoolOffline() {
    log.info("pool size {}", successPools.size());

    if (CollectionUtils.isEmpty(successPools)) {
      return;
    }

    if (successPools.size() >= batchSize ||
        idleInsert.getRetry() == BigInteger.TWO.intValue()) {
      insertBatch(successPools);
      restartPointer();
      return;
    }

    idleInsert.setSize(idleInsert.getSize());
    idleInsert.setRetry(idleInsert.getRetry() + 1);
  }

  private void restartPointer() {
    idleInsert.setSize(0);
    idleInsert.setRetry(0);
  }

  @EventListener
  public void handleSuccessfulPoolData(FetchPoolDataSuccess fetchData) {
    successPools.add(fetchData.getPoolData());
  }


  private synchronized void insertBatch(Set<PoolData> successPools) {
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


            if (existSavingPoolData.isEmpty()){
              mapPoolOfflineData(pod).ifPresent(savingPoolData::add);
            }
          }

      );
    });
  }

  private Optional<PoolOfflineHashProjection> findPoolWithSameHash(
      Set<PoolOfflineHashProjection> existedPoolData, PoolData pod) {
    return existedPoolData.stream()
        .filter(exPod -> exPod.getHash().equals(pod.getHash()) &&
            pod.getPoolId().equals(exPod.getPoolId()))
        .findFirst();
  }

  private void handleEmptyPoolOfflineData(Set<PoolOfflineData> savingPoolData) {
    savingPoolData.addAll(successPools.stream()
        .map(this::mapPoolOfflineData)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList()));
  }

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

  private Optional<PoolOfflineData> buildOfflineData(PoolData poolData, Map<String, Object> map) {

    String name = null;
    PoolHash poolHash = poolHashRepository.getReferenceById(poolData.getPoolId());
    if (ObjectUtils.isEmpty(map.get(POOL_NAME))) {
      name = poolHash.getView();
    } else {
      name = map.get(POOL_NAME).toString();
    }
    String jsonFormated = new String(poolData.getJson()).replace("\n", "")
        .replace("\\t+", "")
        .replace("\\s+", "")
        .strip();
    return Optional.of(PoolOfflineData.builder()
        .poolId(poolData.getPoolId())
        .pmrId(poolData.getMetadataRefId())
        .pool(poolHashRepository.getReferenceById(poolData.getPoolId()))
        .poolMetadataRef(poolMetadataRefRepository.getReferenceById(poolData.getMetadataRefId()))
        .hash(poolData.getHash())
        .poolName(name)
        .tickerName(map.get(TICKER).toString())
        .json(jsonFormated)
        .bytes(poolData.getJson())
        .build());
  }
}
