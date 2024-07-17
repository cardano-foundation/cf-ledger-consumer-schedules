package org.cardanofoundation.job.schedules;

import java.util.List;
import java.util.Map;
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

import org.cardanofoundation.explorer.common.entity.compositeKey.LatestVotingProcedureId;
import org.cardanofoundation.explorer.common.entity.compositeKey.VotingProcedureId;
import org.cardanofoundation.explorer.common.entity.ledgersync.DRepRegistrationEntity_;
import org.cardanofoundation.explorer.common.entity.ledgersync.LatestVotingProcedure;
import org.cardanofoundation.explorer.common.entity.ledgersync.VotingProcedure;
import org.cardanofoundation.job.mapper.VotingProcedureMapper;
import org.cardanofoundation.job.repository.ledgersync.LatestVotingProcedureRepository;
import org.cardanofoundation.job.repository.ledgersync.VotingProcedureRepository;

@Service
@RequiredArgsConstructor
@Log4j2
@ConditionalOnProperty(
    value = "jobs.governance-info.enabled",
    matchIfMissing = true,
    havingValue = "true")
public class VotingProcedureSchedule {

  private final VotingProcedureRepository votingProcedureRepository;
  private final LatestVotingProcedureRepository latestVotingProcedureRepository;

  private final VotingProcedureMapper votingProcedureMapper;

  private static final int DEFAULT_PAGE_SIZE = 1000;

  @Scheduled(cron = "-")
  @Transactional
  void syncUpLatestVotingProcedure() {
    long startTime = System.currentTimeMillis();
    log.info("Sync up Latest Voting Procedure Job: -------Start------");

    Long latestSlot = latestVotingProcedureRepository.findLatestSlotOfVotingProcedure().orElse(0L);
    Pageable pageable =
        PageRequest.of(
            0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, DRepRegistrationEntity_.SLOT));

    Slice<VotingProcedure> votingProcedureSlice =
        votingProcedureRepository.getVotingProcedureBySlotIsGreaterThanEqual(latestSlot, pageable);
    saveLatestVotingProcedure(votingProcedureSlice.getContent());

    while (votingProcedureSlice.hasNext()) {
      pageable = votingProcedureSlice.nextPageable();
      votingProcedureSlice =
          votingProcedureRepository.getVotingProcedureBySlotIsGreaterThanEqual(
              latestSlot, pageable);
      saveLatestVotingProcedure(votingProcedureSlice.getContent());
    }

    log.info(
        "Sync up Latest Voting Procedure Job: -------End------, time: {} ms",
        System.currentTimeMillis() - startTime);
  }

  void saveLatestVotingProcedure(List<VotingProcedure> votingProcedureList) {
    log.info("Processing {} voting procedures", votingProcedureList.size());
    List<LatestVotingProcedureId> votingProcedureIds =
        votingProcedureList.stream()
            .map(votingProcedure -> fromVotingProcedureId(votingProcedure.getVotingProcedureId()))
            .collect(Collectors.toList());

    Map<LatestVotingProcedureId, LatestVotingProcedure> latestVotingProcedureMap =
        latestVotingProcedureRepository.getAllByIdIn(votingProcedureIds).stream()
            .collect(
                Collectors.toMap(LatestVotingProcedure::getVotingProcedureId, Function.identity()));

    votingProcedureList.forEach(
        votingProcedure -> {
          LatestVotingProcedureId latestVotingProcedureId =
              fromVotingProcedureId(votingProcedure.getVotingProcedureId());
          LatestVotingProcedure latestVotingProcedure =
              latestVotingProcedureMap.get(latestVotingProcedureId);
          if (latestVotingProcedure == null) {
            latestVotingProcedure = votingProcedureMapper.fromVotingProcedure(votingProcedure);
            latestVotingProcedure.setRepeatVote(false);
          } else if (!latestVotingProcedure.getId().equals(votingProcedure.getId())) {
            votingProcedureMapper.updateByVotingProcedure(latestVotingProcedure, votingProcedure);
            latestVotingProcedure.setRepeatVote(true);
          }

          latestVotingProcedureMap.put(latestVotingProcedureId, latestVotingProcedure);
        });

    log.info("Saving {} latest voting procedures", latestVotingProcedureMap.size());
    latestVotingProcedureRepository.saveAll(latestVotingProcedureMap.values());
  }

  private LatestVotingProcedureId fromVotingProcedureId(VotingProcedureId votingProcedureId) {
    return LatestVotingProcedureId.builder()
        .govActionTxHash(votingProcedureId.getGovActionTxHash())
        .govActionIndex(votingProcedureId.getGovActionIndex())
        .voterHash(votingProcedureId.getVoterHash())
        .build();
  }
}
