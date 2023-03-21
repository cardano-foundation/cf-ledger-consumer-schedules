package com.sotatek.cardano.job.service.impl;

import com.sotatek.cardano.job.service.interfaces.EpochStakeService;
import com.sotatek.cardano.job.service.interfaces.KafkaService;
import com.sotatek.cardano.ledgersync.common.certs.StakeCredentialType;
import com.sotatek.cardano.ledgersync.common.constant.Constant;
import com.sotatek.cardano.ledgersync.common.dto.EpochStakes;
import com.sotatek.cardano.ledgersync.common.dto.Stake;
import com.sotatek.cardano.ledgersync.util.HexUtil;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class EpochStakeServiceImpl implements EpochStakeService {

  public static final String STATE_BEFORE = "stateBefore";
  public static final String P_STAKE_MARK = "pstakeMark";
  public static final String ES_SNAPSHOTS = "esSnapshots";
  public static final String STAKE = "stake";
  public static final String SCRIPT_HASH = "script hash";
  public static final String KEY_HASH = "key hash";
  public static final String DELEGATIONS = "delegations";
  public static final String LAST_EPOCH = "lastEpoch";
  @Value("${application.network-magic}")
  Integer network;
  @Value("${application.kafka.topic.epochStake}")
  String kafkaTopic;
  private final KafkaService kafkaService;

  @Override
  @SuppressWarnings("unchecked")
  public void handleEpochStake(LinkedHashMap<String, Object> ledgerState) {
    Integer activeEpoch = (Integer) ledgerState.get(LAST_EPOCH) + BigInteger.ONE.intValue();
    var stateBefore = (LinkedHashMap<String, Object>) ledgerState.get(STATE_BEFORE);
    var esSnapshots = (LinkedHashMap<String, Object>) stateBefore.get(ES_SNAPSHOTS);
    var stakeMark = (LinkedHashMap<String, Object>) esSnapshots.get(P_STAKE_MARK);
    var stakes = (ArrayList<Object>) stakeMark.get(STAKE);
    var delegations = (ArrayList<Object>) stakeMark.get(DELEGATIONS);
    var delegationStakes = delegations.stream()
        .map(delegation -> (ArrayList<Object>) delegation)
        .collect(Collectors.toMap(delegation -> {
              var stake = (LinkedHashMap<String, Object>) delegation.get(BigInteger.ZERO.intValue());
              var key = "";
              if (!stake.containsKey(SCRIPT_HASH)) {
                key = getStakeAddressHash(stake.get(KEY_HASH).toString(),
                    StakeCredentialType.ADDR_KEYHASH);
              } else {
                key = getStakeAddressHash(stake.get(SCRIPT_HASH).toString(),
                    StakeCredentialType.SCRIPT_HASH);
              }
              return key;
            },
            delegation -> delegation.get(BigInteger.ONE.intValue()).toString()));

    var epochStakes = stakes
        .stream()
        .map(stake -> (ArrayList<Object>) stake)
        .map(stake -> {
          var amount = new BigInteger(stake.get(BigInteger.ONE.intValue()).toString());
          var pool = ((LinkedHashMap<String, Object>) stake.get(BigInteger.ZERO.intValue()));
          var stakeKey = "";
          if (!pool.containsKey(SCRIPT_HASH)) {
            stakeKey = getStakeAddressHash(pool.get(KEY_HASH).toString(),
                StakeCredentialType.ADDR_KEYHASH);
          } else {
            stakeKey = getStakeAddressHash(pool.get(SCRIPT_HASH).toString(),
                StakeCredentialType.SCRIPT_HASH);
          }

          return Stake.builder()
              .poolHash(delegationStakes.get(stakeKey))
              .stakeKey(stakeKey)
              .amount(amount)
              .build();
        })
        .collect(Collectors.toSet());

    kafkaService.sendMessage(kafkaTopic,
        String.valueOf(activeEpoch),
        EpochStakes.builder()
            .epoch(activeEpoch)
            .stakes(epochStakes)
            .build());
  }

  private String getStakeAddressHash(String hash, StakeCredentialType type) {

    int stakeHeader = type.equals(StakeCredentialType.ADDR_KEYHASH) ? 0b1110_0000 : 0b1111_0000;
    int networkId = Constant.isTestnet(network) ? 0 : 1;
    stakeHeader = stakeHeader | networkId;
    return String.join("", HexUtil.encodeHexString(new byte[]{(byte) stakeHeader}), hash);
  }
}
