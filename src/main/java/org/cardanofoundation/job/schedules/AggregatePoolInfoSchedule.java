package org.cardanofoundation.job.schedules;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.common.entity.explorer.AggregatePoolInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.PoolHash;
import org.cardanofoundation.explorer.common.entity.ledgersync.enumeration.Vote;
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
// @Component
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

    Map<Long, Map<Vote, Long>> poolVoteMap =
        latestVotingProcedureMap.entrySet().parallelStream()
            .filter(entry -> poolHashMap.get(entry.getKey()) != null)
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    entry ->
                        entry.getValue().parallelStream()
                            .collect(
                                Collectors.groupingBy(
                                    LatestVotingProcedureProjection::getVote,
                                    Collectors.counting()))));

    Map<Long, Double> governanceParticipationRate =
        poolVoteMap.entrySet().parallelStream()
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    entry -> {
                      Long yesVotes = entry.getValue().getOrDefault(Vote.YES, 0L);
                      Long noVotes = entry.getValue().getOrDefault(Vote.NO, 0L);
                      Long abtainVotes = entry.getValue().getOrDefault(Vote.ABSTAIN, 0L);
                      Double result =
                          (yesVotes + noVotes) * 1.0 / (yesVotes + noVotes + abtainVotes);
                      return result;
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
