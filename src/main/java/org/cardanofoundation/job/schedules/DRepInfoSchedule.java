package org.cardanofoundation.job.schedules;

import static org.apache.commons.math3.util.Precision.round;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.common.entity.enumeration.DRepActionType;
import org.cardanofoundation.explorer.common.entity.enumeration.DRepStatus;
import org.cardanofoundation.explorer.common.entity.ledgersync.DRepInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.DRepRegistrationEntity;
import org.cardanofoundation.explorer.common.entity.ledgersync.DRepRegistrationEntity_;
import org.cardanofoundation.explorer.common.entity.ledgersync.GovActionProposal;
import org.cardanofoundation.job.mapper.DRepMapper;
import org.cardanofoundation.job.projection.DelegationVoteProjection;
import org.cardanofoundation.job.projection.LatestDrepVotingProcedureProjection;
import org.cardanofoundation.job.projection.LatestEpochVotingProcedureProjection;
import org.cardanofoundation.job.projection.StakeBalanceProjection;
import org.cardanofoundation.job.repository.ledgersync.DRepInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.DRepRegistrationRepository;
import org.cardanofoundation.job.repository.ledgersync.DelegationVoteRepository;
import org.cardanofoundation.job.repository.ledgersync.EpochParamRepository;
import org.cardanofoundation.job.repository.ledgersync.EpochRepository;
import org.cardanofoundation.job.repository.ledgersync.GovActionProposalRepository;
import org.cardanofoundation.job.repository.ledgersync.LatestVotingProcedureRepository;
import org.cardanofoundation.job.repository.ledgersyncagg.StakeAddressBalanceRepository;

@Service
@RequiredArgsConstructor
@Log4j2
@ConditionalOnProperty(
    value = "jobs.drep-info.enabled",
    matchIfMissing = true,
    havingValue = "true")
public class DRepInfoSchedule {
  private final DRepInfoRepository dRepInfoRepository;
  private final DRepRegistrationRepository dRepRegistrationRepository;
  private final DelegationVoteRepository delegationVoteRepository;
  private final EpochParamRepository epochParamRepository;
  private final LatestVotingProcedureRepository latestVotingProcedureRepository;
  private final StakeAddressBalanceRepository stakeAddressBalanceRepository;
  private final GovActionProposalRepository govActionProposalRepository;
  private final EpochRepository epochRepository;

  private final DRepMapper dRepMapper;
  private static final int DEFAULT_PAGE_SIZE = 100;
  // round the double value to 4 decimal places
  private static final int ROUND_SCALE = 4;

  @Scheduled(fixedRateString = "${jobs.drep-info.fixed-delay}")
  @Transactional
  public void syncUpDRepInfo() {
    long startTime = System.currentTimeMillis();
    log.info("Scheduled Drep Info Job: -------Start------");
    Long currentEpoch = epochRepository.findMaxEpochNo().longValue();
    Long dRepActivity = epochParamRepository.findDRepActivityByEpochNo(currentEpoch).orElse(null);
    if (Objects.isNull(dRepActivity)) {
      log.error("DRep Activity param is null, please check the data");
      log.info("Update DRep Info failed, takes: [{} ms]", System.currentTimeMillis() - startTime);
      log.info("Scheduled Drep Info Job: -------End------");
      return;
    }

    Pageable pageable =
        PageRequest.of(
            0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, DRepRegistrationEntity_.SLOT));
    Slice<DRepRegistrationEntity> dRepRegistrationEntitySlice =
        dRepRegistrationRepository.findAll(pageable);
    saveDRepInfo(dRepRegistrationEntitySlice.getContent(), currentEpoch, dRepActivity);

    while (dRepRegistrationEntitySlice.hasNext()) {
      pageable = dRepRegistrationEntitySlice.nextPageable();
      dRepRegistrationEntitySlice = dRepRegistrationRepository.findAll(pageable);
      saveDRepInfo(dRepRegistrationEntitySlice.getContent(), currentEpoch, dRepActivity);
    }
    log.info(
        "Update DRep Info successfully, takes: [{} ms]", System.currentTimeMillis() - startTime);
    log.info("Scheduled Drep Info Job: -------End------");
  }

  private Map<String, Double> getGovernanceParticipationRateMap(
      List<DRepRegistrationEntity> dRepRegistrationEntityList) {
    Set<String> dRepHashes =
        dRepRegistrationEntityList.stream()
            .map(DRepRegistrationEntity::getDrepHash)
            .collect(Collectors.toSet());

    List<GovActionProposal> govActionProposalList = govActionProposalRepository.findAll();

    Map<String, List<LatestDrepVotingProcedureProjection>> latestVotingProcedureNotFilterMap =
        latestVotingProcedureRepository.findAllByVoterHashIn(dRepHashes).stream()
            .collect(Collectors.groupingBy(LatestDrepVotingProcedureProjection::getVoterHash));

    Map<String, Long> drepBlockTimeMap =
        dRepRegistrationEntityList.stream()
            .collect(
                Collectors.toMap(
                    DRepRegistrationEntity::getDrepHash,
                    DRepRegistrationEntity::getBlockTime,
                    (a, b) -> b));

    // filter gov action that submitted before the pool had registered
    Map<String, List<LatestDrepVotingProcedureProjection>> latestVotingProcedureMap =
        latestVotingProcedureNotFilterMap.entrySet().parallelStream()
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    entry -> {
                      Long blockTime = drepBlockTimeMap.getOrDefault(entry.getKey(), 0L);
                      return entry.getValue().stream()
                          .filter(votingProcedure -> votingProcedure.getBlockTime() >= blockTime)
                          .collect(Collectors.toList());
                    }));

    Map<String, Long> countVoteMap =
        latestVotingProcedureMap.entrySet().parallelStream()
            .collect(Collectors.toMap(Entry::getKey, entry -> (long) entry.getValue().size()));

    return countVoteMap.entrySet().parallelStream()
        .collect(
            Collectors.toMap(
                Entry::getKey,
                entry -> {
                  Long blockTime = drepBlockTimeMap.getOrDefault(entry.getKey(), 0L);
                  long countOfGovActionThatAllowedToVoteForDRep =
                      govActionProposalList.stream()
                          .filter(
                              govActionProposal -> govActionProposal.getBlockTime() >= blockTime)
                          .count();

                  if (countOfGovActionThatAllowedToVoteForDRep == 0) {
                    return 0.0;
                  }

                  return round(
                      entry.getValue() * 1.0 / (countOfGovActionThatAllowedToVoteForDRep),
                      ROUND_SCALE);
                }));
  }

  private void saveDRepInfo(
      List<DRepRegistrationEntity> dRepRegistrationEntityList,
      Long currentEpoch,
      Long dRepActivity) {
    log.info("Processing {} DRep registration entities", dRepRegistrationEntityList.size());
    Set<String> drepHashSet =
        dRepRegistrationEntityList.stream()
            .map(DRepRegistrationEntity::getDrepHash)
            .collect(Collectors.toSet());

    List<LatestEpochVotingProcedureProjection> latestEpochGroupByVoterHashList =
        latestVotingProcedureRepository.findAllByVoterHashAndEpochNo(
            currentEpoch - dRepActivity, drepHashSet);

    Map<String, Long> latestEpochMap =
        latestEpochGroupByVoterHashList.stream()
            .collect(
                Collectors.toMap(
                    LatestEpochVotingProcedureProjection::getVoterHash,
                    LatestEpochVotingProcedureProjection::getEpoch));

    List<DelegationVoteProjection> delegationVoteProjectionList =
        delegationVoteRepository.findAllByDRepHashIn(drepHashSet);

    Map<String, BigInteger> activeStakeMap = calculateActiveStake(delegationVoteProjectionList);

    Map<String, List<DelegationVoteProjection>> delegationVoteMap =
        delegationVoteProjectionList.stream()
            .collect(Collectors.groupingBy(DelegationVoteProjection::getDrepHash));

    Map<String, Long> countDelegation =
        delegationVoteMap.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    entry -> {
                      List<DelegationVoteProjection> list = entry.getValue();
                      Set<String> fieldNameSet =
                          list.stream()
                              .map(DelegationVoteProjection::getAddress)
                              .collect(Collectors.toSet());
                      return Long.valueOf(fieldNameSet.size());
                    }));

    Map<String, DRepInfo> dRepInfoMap =
        dRepInfoRepository.findAllByDrepHashIn(drepHashSet).stream()
            .collect(Collectors.toMap(DRepInfo::getDrepHash, Function.identity()));

    Map<String, Double> governanceParticipationRateMap =
        getGovernanceParticipationRateMap(dRepRegistrationEntityList);

    dRepRegistrationEntityList.forEach(
        dRepRegistrationEntity -> {
          DRepInfo dRepInfo = dRepInfoMap.get(dRepRegistrationEntity.getDrepHash());

          if (dRepInfo == null) {
            if (dRepRegistrationEntity.getType().equals(DRepActionType.REG_DREP_CERT)) {
              dRepInfo = dRepMapper.fromDRepRegistration(dRepRegistrationEntity);
              dRepInfo.setCreatedAt(dRepRegistrationEntity.getBlockTime());
              dRepInfo.setUpdatedAt(dRepRegistrationEntity.getBlockTime());
              // TODO calculate live stake, active vote stake, delegators
              dRepInfo.setActiveVoteStake(null);
              dRepInfo.setLiveStake(null);
              dRepInfo.setDelegators(0);

              dRepInfoMap.put(dRepInfo.getDrepHash(), dRepInfo);
            } else {
              log.error("DRep hash {} not registered yet!", dRepRegistrationEntity.getDrepHash());
              throw new RuntimeException("DRep hash not registered yet!");
            }
          } else {
            dRepMapper.updateByDRepRegistration(dRepInfo, dRepRegistrationEntity);
            dRepInfo.setUpdatedAt(dRepRegistrationEntity.getBlockTime());
          }

          dRepInfo.setGovParticipationRate(
              governanceParticipationRateMap.getOrDefault(dRepInfo.getDrepHash(), 0.0));

          dRepInfo.setActiveVoteStake(
              activeStakeMap.getOrDefault(dRepInfo.getDrepHash(), BigInteger.ZERO));
          dRepInfo.setLiveStake(null);
          dRepInfo.setVotingPower(null);

          dRepInfo.setDelegators(
              countDelegation.getOrDefault(dRepInfo.getDrepHash(), 0L).intValue());
          // if the certificate is unregistered, set the status to retired otherwise set to active
          // as a default
          dRepInfo.setStatus(
              dRepRegistrationEntity.getType().equals(DRepActionType.UNREG_DREP_CERT)
                  ? DRepStatus.RETIRED
                  : DRepStatus.ACTIVE);
          // Following the CIP-1694 document
          // (https://github.com/cardano-foundation/CIPs/blob/master/CIP-1694/README.md)
          // ***Specifically, if a DRep does not submit any votes for drepActivity-many epochs, the
          // DRep is considered inactive,
          // where drepActivity is a new protocol parameter. Inactive DReps no longer count towards
          // the active voting stake
          // but can become active again for drepActivity-many epochs by voting on any governance
          // action or submitting a DRep update certificate.***

          // If the epoch of the certificate is less than the value of the current epoch minus dRep
          // activity,
          // check if the latest activity in the voting procedure is less than the current epoch
          // minus dRep activity.
          // If so, set the status to inactive; otherwise, set the status to active.

          // If the epoch of the certificate is greater than the value of the current epoch minus
          // dRep activity,
          // set the status to active, as one condition for being considered active is submitting
          // any DRep update certificate.
          if (!dRepInfo.getStatus().equals(DRepStatus.RETIRED)
              && dRepRegistrationEntity.getEpoch() < currentEpoch - dRepActivity) {
            Long latestEpoch = latestEpochMap.getOrDefault(dRepInfo.getDrepHash(), 0L);
            dRepInfo.setStatus(
                currentEpoch - dRepActivity > latestEpoch
                    ? DRepStatus.INACTIVE
                    : DRepStatus.ACTIVE);
          }
        });

    dRepInfoRepository.saveAll(dRepInfoMap.values());
  }

  private Map<String, BigInteger> calculateActiveStake(
      List<DelegationVoteProjection> delegationVoteProjectionList) {
    Map<String, Set<String>> delegatorMap =
        delegationVoteProjectionList.stream()
            .collect(
                Collectors.groupingBy(
                    DelegationVoteProjection::getDrepHash,
                    Collectors.mapping(DelegationVoteProjection::getAddress, Collectors.toSet())));

    Set<String> stakeAddressSet =
        delegationVoteProjectionList.stream()
            .map(DelegationVoteProjection::getAddress)
            .collect(Collectors.toSet());

    List<StakeBalanceProjection> stakeAddressList =
        stakeAddressBalanceRepository.findStakeAddressBalanceByStakeAddressIn(stakeAddressSet);

    Map<String, BigInteger> stakeAddressBalanceMap =
        stakeAddressList.stream()
            .collect(
                Collectors.toMap(
                    StakeBalanceProjection::getStakeAddress, StakeBalanceProjection::getBalance));

    return delegatorMap.entrySet().stream()
        .collect(
            Collectors.toMap(
                Entry::getKey,
                entry -> {
                  Set<String> delegators = entry.getValue();
                  return delegators.stream()
                      .filter(Objects::nonNull)
                      .map(address -> stakeAddressBalanceMap.getOrDefault(address, BigInteger.ZERO))
                      .reduce(BigInteger.ZERO, BigInteger::add);
                }));
  }
}
