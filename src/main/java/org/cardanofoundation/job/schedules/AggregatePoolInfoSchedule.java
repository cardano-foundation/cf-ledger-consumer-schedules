package org.cardanofoundation.job.schedules;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.common.entity.enumeration.GovActionType;
import org.cardanofoundation.explorer.common.entity.enumeration.VoterType;
import org.cardanofoundation.explorer.common.entity.ledgersync.AggregatePoolInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.GovActionProposal;
import org.cardanofoundation.explorer.common.entity.ledgersync.PoolHash;
import org.cardanofoundation.job.projection.LatestVotingProcedureProjection;
import org.cardanofoundation.job.projection.PoolCountProjection;
import org.cardanofoundation.job.projection.PoolHashProjection;
import org.cardanofoundation.job.projection.PoolInfoProjection;
import org.cardanofoundation.job.repository.ledgersync.AggregatePoolInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.EpochRepository;
import org.cardanofoundation.job.repository.ledgersync.GovActionProposalRepository;
import org.cardanofoundation.job.repository.ledgersync.LatestVotingProcedureRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolHashRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolInfoRepository;
import org.cardanofoundation.job.service.DelegationService;
import org.cardanofoundation.job.service.FetchRewardDataService;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@ConditionalOnProperty(
        value = "jobs.aggregate-pool-info.enabled",
        matchIfMissing = true,
        havingValue = "true")
public class AggregatePoolInfoSchedule {

  final DelegationService delegatorService;

  final BlockRepository blockRepository;
  final AggregatePoolInfoRepository aggregatePoolInfoRepository;
  final PoolHashRepository poolHashRepository;
  final GovActionProposalRepository govActionProposalRepository;
  final LatestVotingProcedureRepository latestVotingProcedureRepository;
  final EpochRepository epochRepository;
  final FetchRewardDataService fetchRewardDataService;
  final PoolInfoRepository poolInfoRepository;

  @Scheduled(fixedDelayString = "${jobs.aggregate-pool-info.fixed-delay}")
  @Transactional
  public void updateAggregatePoolInfoJob() {
    long startTime = System.currentTimeMillis();

    Boolean useKoi0s = fetchRewardDataService.isKoiOs();
    Map<Long, PoolHash> poolHashMap =
        poolHashRepository.findAll().parallelStream()
            .collect(Collectors.toMap(PoolHash::getId, Function.identity()));

    Map<Long, AggregatePoolInfo> aggregatePoolInfoMap =
        aggregatePoolInfoRepository.findAllByPoolIdIn(poolHashMap.keySet()).parallelStream()
            .collect(Collectors.toMap(AggregatePoolInfo::getPoolId, Function.identity()));

    aggregatePoolInfoMap.putAll(
        poolHashMap.keySet().parallelStream()
            .filter(poolId -> !aggregatePoolInfoMap.containsKey(poolId))
            .collect(
                Collectors.toMap(
                    poolId -> poolId,
                    poolId -> AggregatePoolInfo.builder().poolId(poolId).build())));

    Map<Long, Integer> livePoolDelegatorsCountMap =
        delegatorService.getAllLivePoolDelegatorsCount().parallelStream()
            .collect(
                Collectors.toMap(
                    PoolCountProjection::getPoolId, PoolCountProjection::getCountValue));

    Map<Long, Integer> blockLifeTimeMap =
        blockRepository.getCountBlockByPools().parallelStream()
            .collect(
                Collectors.toMap(
                    PoolCountProjection::getPoolId, PoolCountProjection::getCountValue));

    Map<Long, Integer> blockInEpochMap =
        blockRepository.getAllCountBlockInCurrentEpoch().parallelStream()
            .collect(
                Collectors.toMap(
                    PoolCountProjection::getPoolId, PoolCountProjection::getCountValue));

    Map<Long, Double> governanceParticipationRateMap = getGovernanceParticipationRateMap();

    Map<Long, Double> votingPowerMap = getVotingPowerMap(useKoi0s);

    Timestamp currentTime =
        Timestamp.valueOf(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));

    aggregatePoolInfoMap.entrySet().parallelStream()
        .forEach(
            entry -> {
              AggregatePoolInfo aggregatePoolInfo = entry.getValue();
              aggregatePoolInfo.setDelegatorCount(
                  livePoolDelegatorsCountMap.getOrDefault(entry.getKey(), 0));
              aggregatePoolInfo.setBlockLifeTime(blockLifeTimeMap.getOrDefault(entry.getKey(), 0));
              aggregatePoolInfo.setBlockInEpoch(blockInEpochMap.getOrDefault(entry.getKey(), 0));
              aggregatePoolInfo.setUpdateTime(currentTime);
              aggregatePoolInfo.setPoolId(entry.getKey());
              if (useKoi0s) {
                aggregatePoolInfo.setVotingPower(votingPowerMap.getOrDefault(entry.getKey(), null));
              }
              aggregatePoolInfo.setGovernanceParticipationRate(
                  governanceParticipationRateMap.getOrDefault(entry.getKey(), 0.0));
            });

    aggregatePoolInfoRepository.saveAll(aggregatePoolInfoMap.values());
    log.info(
        "Update aggregate pool info done! Time taken: {} ms",
        System.currentTimeMillis() - startTime);
  }

  private Map<Long, Double> getGovernanceParticipationRateMap() {

    List<GovActionType> govActionTypeList = new ArrayList<>(List.of(GovActionType.values()));

    /* Gov Action that not allowed vote by SPOs
     * refer: CIP1694 */
    govActionTypeList.remove(GovActionType.TREASURY_WITHDRAWALS_ACTION);
    govActionTypeList.remove(GovActionType.NEW_CONSTITUTION);
    govActionTypeList.remove(GovActionType.PARAMETER_CHANGE_ACTION);

    List<GovActionProposal> govActionProposalList =
        govActionProposalRepository.getGovActionThatAllowedToVoteForSPO(govActionTypeList);

    // get slot when the pool had registered
    // agr1: poolId, agr2: slot
    Map<Long, Long> poolSlotMap =
        poolHashRepository.getSlotNoWhenFirstDelegation().stream()
            .collect(Collectors.toMap(PoolHashProjection::getPoolId, PoolHashProjection::getSlot));

    Map<Long, List<LatestVotingProcedureProjection>> latestVotingProcedureNotFilterSlotMap =
        latestVotingProcedureRepository
            .findAllByVoterType(VoterType.STAKING_POOL_KEY_HASH)
            .parallelStream()
            .collect(Collectors.groupingBy(LatestVotingProcedureProjection::getPoolId));

    // filter gov action that submitted before the pool had registered
    Map<Long, List<LatestVotingProcedureProjection>> latestVotingProcedureMap =
        latestVotingProcedureNotFilterSlotMap.entrySet().parallelStream()
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    entry -> {
                      Long slot = poolSlotMap.getOrDefault(entry.getKey(), 0L);
                      return entry.getValue().stream()
                          .filter(votingProcedure -> votingProcedure.getSlotGov() >= slot)
                          .collect(Collectors.toList());
                    }));

    Map<Long, Long> countVoteMap =
        latestVotingProcedureMap.entrySet().parallelStream()
            .collect(Collectors.toMap(Entry::getKey, entry -> (long) entry.getValue().size()));

    return countVoteMap.entrySet().parallelStream()
        .collect(
            Collectors.toMap(
                Entry::getKey,
                entry -> {
                  Long slot = poolSlotMap.getOrDefault(entry.getKey(), 0L);
                  long countOfGovActionThatAllowedToVoteForSPO =
                      govActionProposalList.stream()
                          .filter(govActionProposal -> govActionProposal.getSlot() >= slot)
                          .count();
                  return (Double)
                      (entry.getValue() * 1.0 / (countOfGovActionThatAllowedToVoteForSPO));
                }));
  }

  private Map<Long, Double> getVotingPowerMap(Boolean useKoi0s) {
    BigInteger sumOfActiveStake;
    if (useKoi0s) {
      Integer currentEpoch = epochRepository.findMaxEpochNo();
      List<PoolInfoProjection> poolInfoList = poolInfoRepository.findAllByEpochNo(currentEpoch);
      sumOfActiveStake =
          poolInfoList.stream()
              .map(PoolInfoProjection::getActiveStake)
              .filter(Objects::nonNull)
              .reduce(BigInteger.ZERO, BigInteger::add);
      if (sumOfActiveStake.equals(BigInteger.ZERO)) {
        return new HashMap<>();
      }
      return poolInfoList.parallelStream()
          .filter(poolInfoProjection -> poolInfoProjection.getActiveStake() != null)
          .collect(
              Collectors.toMap(
                  PoolInfoProjection::getPoolId,
                  poolInfoProjection ->
                      poolInfoProjection.getActiveStake().doubleValue()
                          / sumOfActiveStake.doubleValue()));
    }
    return new HashMap<>();
  }
}
