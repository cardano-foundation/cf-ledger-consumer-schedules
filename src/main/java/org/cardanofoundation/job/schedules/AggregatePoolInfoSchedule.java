package org.cardanofoundation.job.schedules;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.common.entity.explorer.AggregatePoolInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.GovActionProposal;
import org.cardanofoundation.explorer.common.entity.ledgersync.PoolHash;
import org.cardanofoundation.explorer.common.entity.ledgersync.enumeration.GovActionType;
import org.cardanofoundation.explorer.common.entity.ledgersync.enumeration.VoterType;
import org.cardanofoundation.job.projection.LatestVotingProcedureProjection;
import org.cardanofoundation.job.projection.PoolCountProjection;
import org.cardanofoundation.job.repository.explorer.AggregatePoolInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.GovActionProposalRepository;
import org.cardanofoundation.job.repository.ledgersync.LatestVotingProcedureRepository;
import org.cardanofoundation.job.repository.ledgersync.PoolHashRepository;
import org.cardanofoundation.job.service.DelegationService;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class AggregatePoolInfoSchedule {

  final DelegationService delegatorService;

  final BlockRepository blockRepository;
  final AggregatePoolInfoRepository aggregatePoolInfoRepository;
  final PoolHashRepository poolHashRepository;
  final GovActionProposalRepository govActionProposalRepository;
  final LatestVotingProcedureRepository latestVotingProcedureRepository;

  @Scheduled(fixedDelayString = "${jobs.aggregate-pool-info.fixed-delay}")
  @Transactional
  public void updateAggregatePoolInfoJob() {
    long startTime = System.currentTimeMillis();
    Map<Long, PoolHash> poolHashMap =
        poolHashRepository.findAll().parallelStream()
            .collect(Collectors.toMap(PoolHash::getId, Function.identity()));
    Map<Long, List<LatestVotingProcedureProjection>> latestVotingProcedureMap =
        latestVotingProcedureRepository
            .findAllByVoterType(VoterType.STAKING_POOL_KEY_HASH)
            .parallelStream()
            .collect(Collectors.groupingBy(LatestVotingProcedureProjection::getPoolId));

    List<GovActionType> govActionTypeList = new ArrayList<>(List.of(GovActionType.values()));

    govActionTypeList.remove(GovActionType.TREASURY_WITHDRAWALS_ACTION);
    govActionTypeList.remove(GovActionType.NEW_CONSTITUTION);
    govActionTypeList.remove(GovActionType.PARAMETER_CHANGE_ACTION);

    List<GovActionProposal> govActionProposalList =
        govActionProposalRepository.getGovActionThatAllowedToVoteForSPO(govActionTypeList);

    Map<Long, Long> poolVoteMap =
        latestVotingProcedureMap.entrySet().parallelStream()
            .collect(
                Collectors.toMap(
                    Entry::getKey, entry -> (Long) entry.getValue().parallelStream().count()));

    Map<Long, Double> governanceParticipationRate =
        poolVoteMap.entrySet().parallelStream()
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    entry -> {
                      Long slot =
                          poolHashRepository.getSlotNoWhenFirstDelegationByPoolHash(
                              poolHashMap.get(entry.getKey()).getHashRaw());
                      long countOfGovActionThatAllowedToVoteForSPO =
                          govActionProposalList.stream()
                              .filter(govActionProposal -> govActionProposal.getEpoch() < slot)
                              .count();
                      return (Double)
                          (entry.getValue() * 1.0 / (countOfGovActionThatAllowedToVoteForSPO));
                    }));

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
              aggregatePoolInfo.setGovernanceParticipationRate(
                  governanceParticipationRate.getOrDefault(entry.getKey(), 0.0));
            });

    aggregatePoolInfoRepository.saveAll(aggregatePoolInfoMap.values());
    log.info(
        "Update aggregate pool info done! Time taken: {} ms",
        System.currentTimeMillis() - startTime);
  }
}
