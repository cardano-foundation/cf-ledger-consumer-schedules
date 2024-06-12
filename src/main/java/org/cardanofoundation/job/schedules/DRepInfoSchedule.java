package org.cardanofoundation.job.schedules;

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
import org.cardanofoundation.explorer.common.entity.explorer.DRepInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.DRepRegistrationEntity;
import org.cardanofoundation.explorer.common.entity.ledgersync.DRepRegistrationEntity_;
import org.cardanofoundation.job.mapper.DRepMapper;
import org.cardanofoundation.job.projection.DelegationVoteProjection;
import org.cardanofoundation.job.projection.LatestEpochVotingProcedureProjection;
import org.cardanofoundation.job.projection.StakeBalanceProjection;
import org.cardanofoundation.job.repository.explorer.DRepInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.DRepRegistrationRepository;
import org.cardanofoundation.job.repository.ledgersync.DelegationVoteRepository;
import org.cardanofoundation.job.repository.ledgersync.EpochParamRepository;
import org.cardanofoundation.job.repository.ledgersync.EpochRepository;
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
  private final EpochRepository epochRepository;

  private final DRepMapper dRepMapper;
  private static final int DEFAULT_PAGE_SIZE = 100;

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
          dRepInfo.setActiveVoteStake(
              activeStakeMap.getOrDefault(dRepInfo.getDrepHash(), BigInteger.ZERO));
          dRepInfo.setLiveStake(null);
          dRepInfo.setVotingPower(null);

          dRepInfo.setDelegators(
              countDelegation.getOrDefault(dRepInfo.getDrepHash(), 0L).intValue());
          dRepInfo.setStatus(
              dRepRegistrationEntity.getType().equals(DRepActionType.UNREG_DREP_CERT)
                  ? DRepStatus.RETIRED
                  : DRepStatus.ACTIVE);
          if (!dRepInfo.getStatus().equals(DRepStatus.RETIRED)) {
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
