package org.cardanofoundation.job.schedules;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import org.cardanofoundation.explorer.common.entity.enumeration.DRepStatus;
import org.cardanofoundation.explorer.common.entity.explorer.DRepInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.DRepRegistrationEntity;
import org.cardanofoundation.explorer.common.entity.ledgersync.DRepRegistrationEntity_;
import org.cardanofoundation.explorer.common.entity.ledgersync.enumeration.DRepActionType;
import org.cardanofoundation.job.mapper.DRepMapper;
import org.cardanofoundation.job.projection.DelegationVoteProjection;
import org.cardanofoundation.job.repository.explorer.DRepInfoRepository;
import org.cardanofoundation.job.repository.ledgersync.DRepRegistrationRepository;
import org.cardanofoundation.job.repository.ledgersync.DelegationVoteRepository;

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

  private final DRepMapper dRepMapper;
  private static final int DEFAULT_PAGE_SIZE = 100;

  @Scheduled(fixedRateString = "${jobs.drep-info.fixed-delay}")
  @Transactional
  public void syncUpDRepInfo() {
    long startTime = System.currentTimeMillis();
    log.info("Scheduled Drep Info Job: -------Start------");
    Pageable pageable =
        PageRequest.of(
            0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, DRepRegistrationEntity_.SLOT));
    Slice<DRepRegistrationEntity> dRepRegistrationEntitySlice =
        dRepRegistrationRepository.findAll(pageable);
    saveDRepInfo(dRepRegistrationEntitySlice.getContent());

    while (dRepRegistrationEntitySlice.hasNext()) {
      pageable = dRepRegistrationEntitySlice.nextPageable();
      dRepRegistrationEntitySlice = dRepRegistrationRepository.findAll(pageable);
      saveDRepInfo(dRepRegistrationEntitySlice.getContent());
    }
    log.info(
        "Update DRep Info successfully, takes: [{} ms]", System.currentTimeMillis() - startTime);
    log.info("Scheduled Drep Info Job: -------End------");
  }

  private void saveDRepInfo(List<DRepRegistrationEntity> dRepRegistrationEntityList) {
    log.info("Processing {} DRep registration entities", dRepRegistrationEntityList.size());
    Set<String> drepHashSet =
        dRepRegistrationEntityList.stream()
            .map(DRepRegistrationEntity::getDrepHash)
            .collect(Collectors.toSet());

    List<DelegationVoteProjection> delegationVoteProjectionList =
        delegationVoteRepository.findAllByDRepHashIn(drepHashSet);

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
          }
          dRepInfo.setActiveVoteStake(null);
          dRepInfo.setLiveStake(null);
          dRepInfo.setDelegators(
              countDelegation.getOrDefault(dRepInfo.getDrepHash(), 0L).intValue());
          dRepInfo.setStatus(
              dRepRegistrationEntity.getType().equals(DRepActionType.UNREG_DREP_CERT)
                  ? DRepStatus.INACTIVE
                  : DRepStatus.ACTIVE);
        });

    dRepInfoRepository.saveAll(dRepInfoMap.values());
  }
}
