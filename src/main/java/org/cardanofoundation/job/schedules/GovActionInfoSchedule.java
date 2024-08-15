package org.cardanofoundation.job.schedules;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
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

import org.cardanofoundation.explorer.common.entity.compositeKey.GovActionProposalId;
import org.cardanofoundation.explorer.common.entity.enumeration.CommitteeState;
import org.cardanofoundation.explorer.common.entity.enumeration.GovActionStatus;
import org.cardanofoundation.explorer.common.entity.enumeration.GovActionType;
import org.cardanofoundation.explorer.common.entity.ledgersync.DRepRegistrationEntity_;
import org.cardanofoundation.explorer.common.entity.ledgersync.EpochParam;
import org.cardanofoundation.explorer.common.entity.ledgersync.GovActionProposal;
import org.cardanofoundation.explorer.common.entity.ledgersync.GovActionProposalInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.GovActionProposalInfo_;
import org.cardanofoundation.job.projection.gov.GovActionVoteCountProjection;
import org.cardanofoundation.job.repository.ledgersync.CommitteeRegistrationRepository;
import org.cardanofoundation.job.repository.ledgersync.EpochParamRepository;
import org.cardanofoundation.job.repository.ledgersync.GovActionProposalInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.GovActionProposalRepository;
import org.cardanofoundation.job.repository.ledgersync.LatestVotingProcedureRepository;
import org.cardanofoundation.job.repository.ledgersync.VotingProcedureRepository;

@Service
@RequiredArgsConstructor
@Log4j2
@ConditionalOnProperty(
    value = "jobs.governance-info.enabled",
    matchIfMissing = true,
    havingValue = "true")
public class GovActionInfoSchedule {

  private final EpochParamRepository epochParamRepository;
  private final CommitteeRegistrationRepository committeeRegistrationRepository;
  private final GovActionProposalInfoRepository govActionProposalInfoRepository;
  private final GovActionProposalRepository govActionProposalRepository;
  private final LatestVotingProcedureRepository latestVotingProcedureRepository;
  private final VotingProcedureRepository votingProcedureRepository;

  private static final int DEFAULT_PAGE_SIZE = 1000;

  @Scheduled(fixedRateString = "${jobs.governance-info.fixed-delay}")
  public void syncUpGovActionInfo() {
    long startTime = System.currentTimeMillis();
    log.info("Scheduled Governance Info Job: -------Start------");

    Long latestLvpSlot =
        latestVotingProcedureRepository.findLatestSlotOfVotingProcedure().orElse(0L);
    Long latestVpSlot = votingProcedureRepository.findLatestSlotOfVotingProcedure().orElse(0L);

    if (!latestLvpSlot.equals(latestVpSlot)) {
      log.info(
          "Latest slot of VotingProcedure and LatestVotingProcedure are not same >>>> Skipping the job");
      return;
    }

    Pageable pageable =
        PageRequest.of(
            0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, DRepRegistrationEntity_.SLOT));

    Slice<GovActionProposal> govActionProposalSlice = govActionProposalRepository.findAll(pageable);
    saveGovActionInfo(govActionProposalSlice.getContent());

    if (govActionProposalSlice.hasNext()) {
      while (govActionProposalSlice.hasNext()) {
        pageable = govActionProposalSlice.nextPageable();
        govActionProposalSlice = govActionProposalRepository.findAll(pageable);
        saveGovActionInfo(govActionProposalSlice.getContent());
      }
    }
    addIndexTypeForGovAction();
    log.info(
        "Scheduled Governance Info Job: -------End------, time: {} ms",
        System.currentTimeMillis() - startTime);
  }

  private void addIndexTypeForGovAction() {
    List<GovActionType> govActionTypes = List.of(GovActionType.values());

    govActionTypes.parallelStream()
        .forEach(
            govActionType -> {
              AtomicLong initValue = new AtomicLong(1);
              Pageable pageable =
                  PageRequest.of(
                      0,
                      DEFAULT_PAGE_SIZE,
                      Sort.by(Sort.Direction.ASC, "tx.id", GovActionProposalInfo_.INDEX));
              Slice<GovActionProposalInfo> govActionProposalInfoList =
                  govActionProposalInfoRepository.findByType(govActionType, pageable);
              govActionProposalInfoList.forEach(
                  govActionProposalInfo -> {
                    govActionProposalInfo.setIndexType(
                        BigInteger.valueOf(initValue.getAndIncrement()));
                  });
              govActionProposalInfoRepository.saveAll(govActionProposalInfoList);
              if (govActionProposalInfoList.hasNext()) {
                while (govActionProposalInfoList.hasNext()) {
                  pageable = govActionProposalInfoList.nextPageable();
                  govActionProposalInfoList =
                      govActionProposalInfoRepository.findByType(govActionType, pageable);
                  govActionProposalInfoList.forEach(
                      govActionProposalInfo -> {
                        govActionProposalInfo.setIndexType(
                            BigInteger.valueOf(initValue.getAndIncrement()));
                      });
                  govActionProposalInfoRepository.saveAll(govActionProposalInfoList);
                }
              }
            });
    log.info("Add successfully index for gov action proposal by type");
  }

  void saveGovActionInfo(List<GovActionProposal> govActionProposals) {
    log.info("Processing {} GovActionProposals", govActionProposals.size());
    EpochParam currentEpochParam = epochParamRepository.findCurrentEpochParam();

    Set<GovActionProposalId> govActionProposalIds =
        govActionProposals.stream().map(GovActionProposal::getId).collect(Collectors.toSet());

    Map<GovActionProposalId, GovActionVoteCountProjection> govActionVoteCountMap =
        govActionProposalRepository
            .findGovActionVoteCountByGovActionProposalIdIn(govActionProposalIds)
            .stream()
            .collect(
                Collectors.toMap(
                    GovActionVoteCountProjection::getGovActionProposalId, Function.identity()));

    Map<GovActionProposalId, GovActionProposalInfo> govActionProposalInfoMap =
        govActionProposalInfoRepository.findAllById(govActionProposalIds).stream()
            .collect(Collectors.toMap(GovActionProposalInfo::getId, Function.identity()));

    govActionProposals.forEach(
        govActionProposal -> {
          GovActionProposalId govActionProposalId = govActionProposal.getId();
          GovActionVoteCountProjection govActionVoteCountProjection =
              govActionVoteCountMap.get(govActionProposalId);
          GovActionProposalInfo govActionProposalInfo =
              govActionProposalInfoMap.get(govActionProposalId);

          // if govActionProposalId is not present in the map, then save it into map
          if (govActionProposalInfo == null) {
            govActionProposalInfo =
                GovActionProposalInfo.builder()
                    .txHash(govActionProposal.getTxHash())
                    .index(govActionProposal.getIndex())
                    .votingPower(BigInteger.ZERO) // TODO calculate voting power
                    .expiredEpoch(
                        govActionProposal.getEpoch()
                            + getGovActionLifetime(currentEpochParam.getGovActionLifetime()))
                    .type(govActionProposal.getType())
                    .blockTime(govActionProposal.getBlockTime())
                    .build();
          }

          govActionProposalInfo.setStatus(
              getGovActionStatus(
                  govActionProposalInfo.getStatus(),
                  govActionProposal.getType(),
                  currentEpochParam,
                  govActionVoteCountProjection));

          govActionProposalInfoMap.put(govActionProposalId, govActionProposalInfo);
        });

    log.info("Saving {} GovActionProposalInfo", govActionProposalInfoMap.size());
    govActionProposalInfoRepository.saveAll(govActionProposalInfoMap.values());
  }

  GovActionStatus getGovActionStatus(
      GovActionStatus currentStatus,
      GovActionType govActionType,
      EpochParam currentEpochParam,
      GovActionVoteCountProjection govActionVoteCountProjection) {

    if (currentStatus == GovActionStatus.EXPIRED) {
      return GovActionStatus.EXPIRED;
    }

    EpochParam epochParam = govActionVoteCountProjection.getEpochParam();
    int currentEpoch = currentEpochParam.getEpochNo();

    int expiredEpoch =
        govActionVoteCountProjection.getGapEpochNo()
            + getGovActionLifetime(epochParam.getGovActionLifetime());
    if (currentEpoch < expiredEpoch) {
      return GovActionStatus.OPEN_BALLOT;
    }

    double dRepYesVotes = govActionVoteCountProjection.getNumberOfDRepYesVotes();
    double dRepNoVotes = govActionVoteCountProjection.getNumberOfDRepNoVotes();
    double dRepAbstainVotes = govActionVoteCountProjection.getNumberOfDRepAbstainVotes();
    double poolYesVotes = govActionVoteCountProjection.getNumberOfPoolYesVotes();
    double poolNoVotes = govActionVoteCountProjection.getNumberOfPoolNoVotes();
    double poolAbstainVotes = govActionVoteCountProjection.getNumberOfPoolAbstainVotes();
    double ccRepYesVotes = govActionVoteCountProjection.getNumberOfCCRepYesVotes();
    double ccRepNoVotes = govActionVoteCountProjection.getNumberOfCCRepNoVotes();
    double ccRepAbstainVotes = govActionVoteCountProjection.getNumberOfCCRepAbstainVotes();

    double percentageDRepYesVotes = dRepYesVotes / (dRepYesVotes + dRepNoVotes + dRepAbstainVotes);
    double percentagePoolYesVotes = poolYesVotes / (poolYesVotes + poolNoVotes + poolAbstainVotes);
    double percentageCCRepYesVotes =
        ccRepYesVotes / (ccRepYesVotes + ccRepNoVotes + ccRepAbstainVotes);

    Integer committeeTotalCount =
        committeeRegistrationRepository.countByExpiredEpochNo(expiredEpoch);
    CommitteeState committeeState =
        committeeTotalCount >= currentEpochParam.getCommitteeMinSize().intValue()
            ? CommitteeState.CONFIDENCE
            : CommitteeState.NO_CONFIDENCE;

    switch (govActionType) {
      case NO_CONFIDENCE -> {
        if (percentageDRepYesVotes >= getDoubleValue(epochParam.getPvtMotionNoConfidence())
            && percentagePoolYesVotes >= getDoubleValue(epochParam.getPvtMotionNoConfidence())) {
          return currentEpoch == expiredEpoch ? GovActionStatus.RATIFIED : GovActionStatus.ENACTED;
        } else {
          return GovActionStatus.EXPIRED;
        }
      }

      case UPDATE_COMMITTEE -> {
        if (committeeState == CommitteeState.CONFIDENCE
            && percentageDRepYesVotes >= getDoubleValue(epochParam.getDvtCommitteeNormal())
            && percentagePoolYesVotes >= getDoubleValue(epochParam.getPvtCommitteeNormal())) {
          return currentEpoch == expiredEpoch ? GovActionStatus.RATIFIED : GovActionStatus.ENACTED;
        } else if (committeeState == CommitteeState.NO_CONFIDENCE
            && percentageDRepYesVotes >= getDoubleValue(epochParam.getDvtCommitteeNoConfidence())
            && percentagePoolYesVotes >= getDoubleValue(epochParam.getPvtCommitteeNoConfidence())) {
          return currentEpoch == expiredEpoch ? GovActionStatus.RATIFIED : GovActionStatus.ENACTED;
        } else {
          return GovActionStatus.EXPIRED;
        }
      }

      case NEW_CONSTITUTION -> {
        if (percentageDRepYesVotes >= getDoubleValue(epochParam.getDvtUpdateToConstitution())
            && percentageCCRepYesVotes >= 0) {

          return currentEpoch == expiredEpoch ? GovActionStatus.RATIFIED : GovActionStatus.ENACTED;
        } else {
          return GovActionStatus.EXPIRED;
        }
      }

      case HARD_FORK_INITIATION_ACTION -> {
        if (percentageDRepYesVotes >= getDoubleValue(epochParam.getDvtHardForkInitiation())
            && percentagePoolYesVotes >= getDoubleValue(epochParam.getDvtHardForkInitiation())
            && percentageCCRepYesVotes >= 0) {
          return currentEpoch == expiredEpoch ? GovActionStatus.RATIFIED : GovActionStatus.ENACTED;
        } else {
          return GovActionStatus.EXPIRED;
        }
      }

      case PARAMETER_CHANGE_ACTION -> {
        if (percentageDRepYesVotes >= getDoubleValue(epochParam.getDvtPPGovGroup())
            && percentageCCRepYesVotes >= 0) {
          return currentEpoch == expiredEpoch ? GovActionStatus.RATIFIED : GovActionStatus.ENACTED;
        } else {
          return GovActionStatus.EXPIRED;
        }
      }

      case TREASURY_WITHDRAWALS_ACTION -> {
        if (percentageDRepYesVotes >= getDoubleValue(epochParam.getDvtTreasuryWithdrawal())
            && percentageCCRepYesVotes >= 0) {
          return currentEpoch == expiredEpoch ? GovActionStatus.RATIFIED : GovActionStatus.ENACTED;
        } else {
          return GovActionStatus.EXPIRED;
        }
      }
      case INFO_ACTION -> {
        if (percentageDRepYesVotes == 1
            && percentagePoolYesVotes == 1
            && percentageCCRepYesVotes >= 0) {
          return currentEpoch == expiredEpoch ? GovActionStatus.RATIFIED : GovActionStatus.ENACTED;
        } else {
          return GovActionStatus.EXPIRED;
        }
      }
    }

    return GovActionStatus.EXPIRED;
  }

  int getGovActionLifetime(BigInteger govActionLifetime) {
    return govActionLifetime == null ? 0 : govActionLifetime.intValue();
  }

  double getDoubleValue(Double value) {
    return value == null ? 0 : value;
  }
}
