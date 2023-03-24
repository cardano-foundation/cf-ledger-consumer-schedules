package com.sotatek.cardano.job.service.impl;

import com.sotatek.cardano.common.entity.EpochStake;
import com.sotatek.cardano.common.entity.PoolHash;
import com.sotatek.cardano.common.entity.StakeAddress;
import com.sotatek.cardano.job.repository.EpochStakeRepository;
import com.sotatek.cardano.job.repository.PoolHashRepository;
import com.sotatek.cardano.job.repository.StakeAddressRepository;
import com.sotatek.cardano.job.service.interfaces.EpochStakeService;
import com.sotatek.cardano.ledgersync.common.certs.StakeCredentialType;
import com.sotatek.cardano.ledgersync.common.constant.Constant;
import com.sotatek.cardano.ledgersync.common.dto.EpochStakes;
import com.sotatek.cardano.ledgersync.common.dto.Stake;
import com.sotatek.cardano.ledgersync.util.HexUtil;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class EpochStakeServiceImpl implements EpochStakeService {

  public static final String STATE_BEFORE = "stateBefore";
  public static final String P_STAKE_SET = "pstakeSet";
  public static final String ES_SNAPSHOTS = "esSnapshots";
  public static final String STAKE = "stake";
  public static final String SCRIPT_HASH = "script hash";
  public static final String KEY_HASH = "key hash";
  public static final String DELEGATIONS = "delegations";
  public static final String LAST_EPOCH = "lastEpoch";

  @Value("${application.network-magic}")
  Integer network;
  private final PoolHashRepository poolHashRepository;
  private final StakeAddressRepository stakeAddressRepository;
  private final EpochStakeRepository epochStakeRepository;

  @Override
  @SuppressWarnings("unchecked")
  @Transactional
  public void handleLedgerState(Map<String, Object> ledgerState) {
    int activeEpoch = (Integer) ledgerState.get(LAST_EPOCH);
    var stateBefore = (LinkedHashMap<String, Object>) ledgerState.get(STATE_BEFORE);
    var esSnapshots = (LinkedHashMap<String, Object>) stateBefore.get(ES_SNAPSHOTS);
    var stakeSet = (LinkedHashMap<String, Object>) esSnapshots.get(P_STAKE_SET);
    var stakes = (ArrayList<Object>) stakeSet.get(STAKE);
    var delegations = (ArrayList<Object>) stakeSet.get(DELEGATIONS);
    var delegationStakes = delegations.stream()
        .map(delegation -> (ArrayList<Object>) delegation)
        .collect(Collectors.toMap(delegation -> {
              var hash = (LinkedHashMap<String, Object>) delegation.get(BigInteger.ZERO.intValue());
              return getStakeHash(hash);
            },
            delegation -> delegation.get(BigInteger.ONE.intValue()).toString()));

    var epochStakes = stakes
        .stream()
        .map(stake -> (ArrayList<Object>) stake)
        .map(stake -> {
          var amount = new BigInteger(stake.get(BigInteger.ONE.intValue()).toString());
          var hash = ((LinkedHashMap<String, Object>) stake.get(BigInteger.ZERO.intValue()));
          var stakeKey = getStakeHash(hash);


          return Stake.builder()
              .poolHash(delegationStakes.get(stakeKey))
              .stakeKey(stakeKey)
              .amount(amount)
              .build();
        })
        .collect(Collectors.toSet());

    if (ObjectUtils.isEmpty(epochStakes)) {
      return;
    }

    insertEpochStake(EpochStakes.builder()
        .epoch(activeEpoch + BigInteger.ONE.intValue())
        .stakes(epochStakes)
        .build());
  }

  private String getStakeHash(LinkedHashMap<String, Object> stake) {
    String key;
    if (!stake.containsKey(SCRIPT_HASH)) {
      key = getStakeAddressHash(stake.get(KEY_HASH).toString(),
          StakeCredentialType.ADDR_KEYHASH);
    } else {
      key = getStakeAddressHash(stake.get(SCRIPT_HASH).toString(),
          StakeCredentialType.SCRIPT_HASH);
    }
    return key;
  }

  /**
   * Insert stake addresses delegations to pool on Epoch
   *
   * @param epochStake stake addresses delegation to pool
   */
  private void insertEpochStake(EpochStakes epochStake) {
    var poolsHashRaw = epochStake.getStakes().stream()
        .map(Stake::getPoolHash)
        .collect(Collectors.toSet());

    var stakeAddresses = epochStake.getStakes()
        .stream()
        .map(Stake::getStakeKey)
        .collect(Collectors.toSet());

    Map<String, PoolHash> poolsHashEntities = poolHashRepository.findByHashRawIn(poolsHashRaw)
        .stream()
        .collect(Collectors.toMap(PoolHash::getHashRaw, Function.identity()));

    Map<String, StakeAddress> stakeAddressEntities = stakeAddressRepository.findByHashRawIn(
            stakeAddresses)
        .stream()
        .collect(Collectors.toMap(StakeAddress::getHashRaw, Function.identity()));

    var epochStakesEntities = epochStake.getStakes().stream()
        .map(stake -> EpochStake.builder()
            .epochNo(epochStake.getEpoch())
            .pool(poolsHashEntities.get(stake.getPoolHash()))
            .addr(stakeAddressEntities.get(stake.getStakeKey()))
            .amount(stake.getAmount())
            .build())
        .map(EpochStake.class::cast)
        .collect(Collectors.toList());

    epochStakeRepository.saveAll(epochStakesEntities);
    //updatePoolSize(poolsHashEntities, epochStakesEntities);
  }


  /**
   * Update pool size by total UXTO staked to pool in active epoch
   *
   * @param poolsHash   map pool hash with ket, value (hashRaw, PoolHash)
   * @param epochStakes list epoch stake inserted in current batch
   */
  private void updatePoolSize(Map<String, PoolHash> poolsHash, List<EpochStake> epochStakes) {

    Map<Pair<String, Integer>, Long> poolStake =
        epochStakes.stream()
            .collect(
                Collectors.groupingBy(
                    et -> Pair.of(et.getPool().getHashRaw(), et.getEpochNo()),
                    Collectors.summingLong(et -> et.getAmount().longValue())));

    var poolHashes = poolsHash.values();

    poolHashes.forEach(poolHash -> {
      var maxPoolSizeOnEpochs = poolStake.keySet()
          .stream()
          .filter(pair -> pair.getFirst().equals(poolHash.getHashRaw()))
          .max(Comparator.comparing(Pair::getSecond))
          .stream().findFirst().orElseThrow();

      poolHash.setPoolSize(BigInteger.valueOf(poolStake.get(maxPoolSizeOnEpochs)));
      poolHash.setEpochNo(maxPoolSizeOnEpochs.getSecond());
    });

    poolHashRepository.saveAll(poolHashes);
  }

  /**
   * Get stake address from hash
   *
   * @param hash May be key hash or script hash
   * @param type Stake credential type
   * @return String stake address
   */
  private String getStakeAddressHash(String hash, StakeCredentialType type) {

    int stakeHeader = type.equals(StakeCredentialType.ADDR_KEYHASH) ? 0b1110_0000 : 0b1111_0000;
    int networkId = Constant.isTestnet(network) ? 0 : 1;
    stakeHeader = stakeHeader | networkId;
    return String.join("", HexUtil.encodeHexString(new byte[]{(byte) stakeHeader}), hash);
  }
}
