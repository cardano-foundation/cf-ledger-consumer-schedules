package org.cardanofoundation.job.schedules;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
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
import org.cardanofoundation.explorer.common.entity.enumeration.DataCheckpointType;
import org.cardanofoundation.explorer.common.entity.explorer.DRepInfo;
import org.cardanofoundation.explorer.common.entity.explorer.DataCheckpoint;
import org.cardanofoundation.explorer.common.entity.ledgersync.Block;
import org.cardanofoundation.explorer.common.entity.ledgersync.DRepRegistrationEntity;
import org.cardanofoundation.explorer.common.entity.ledgersync.DRepRegistrationEntity_;
import org.cardanofoundation.explorer.common.entity.ledgersync.enumeration.DRepActionType;
import org.cardanofoundation.job.mapper.DRepMapper;
import org.cardanofoundation.job.repository.explorer.DRepInfoRepository;
import org.cardanofoundation.job.repository.explorer.DataCheckpointRepository;
import org.cardanofoundation.job.repository.ledgersync.BlockRepository;
import org.cardanofoundation.job.repository.ledgersync.DRepRegistrationRepository;

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
  private final DataCheckpointRepository dataCheckpointRepository;
  private final BlockRepository blockRepository;

  private final DRepMapper dRepMapper;
  private static final int DEFAULT_PAGE_SIZE = 100;

  @Scheduled(fixedRateString = "${jobs.drep-info.fixed-delay}")
  @Transactional
  public void syncUpDRepInfo() {
    long startTime = System.currentTimeMillis();
    log.info("Scheduled Drep Info Job: -------Start------");

    Block currentBlock = blockRepository.findLatestBlock().get();
    DataCheckpoint currentDataCheckpoint =
        dataCheckpointRepository
            .findFirstByType(DataCheckpointType.DREP_INFO)
            .orElse(
                DataCheckpoint.builder()
                    .type(DataCheckpointType.DREP_INFO)
                    .blockNo(0L)
                    .slotNo(0L)
                    .build());

    Pageable pageable =
        PageRequest.of(
            0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.ASC, DRepRegistrationEntity_.SLOT));
    Slice<DRepRegistrationEntity> dRepRegistrationEntitySlice =
        dRepRegistrationRepository.findAllBySlotGreaterThan(
            currentDataCheckpoint.getSlotNo(), pageable);

    saveDRepInfo(dRepRegistrationEntitySlice.getContent());

    while (dRepRegistrationEntitySlice.hasNext()) {
      pageable = dRepRegistrationEntitySlice.nextPageable();
      dRepRegistrationEntitySlice =
          dRepRegistrationRepository.findAllBySlotGreaterThan(
              currentDataCheckpoint.getSlotNo(), pageable);
      saveDRepInfo(dRepRegistrationEntitySlice.getContent());
    }

    currentDataCheckpoint.setBlockNo(currentBlock.getBlockNo());
    currentDataCheckpoint.setSlotNo(currentBlock.getSlotNo());
    currentDataCheckpoint.setUpdateTime(new Timestamp(System.currentTimeMillis()));
    dataCheckpointRepository.save(currentDataCheckpoint);

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
              dRepInfo.setLiveStake(BigInteger.ZERO);
              dRepInfo.setActiveVoteStake(BigInteger.ZERO);
              dRepInfo.setDelegators(0);

              dRepInfoMap.put(dRepInfo.getDrepHash(), dRepInfo);
            } else {
              log.error("DRep hash {} not registered yet!", dRepRegistrationEntity.getDrepHash());
              throw new RuntimeException("DRep hash not registered yet!");
            }
          } else {
            dRepMapper.updateByDRepRegistration(dRepInfo, dRepRegistrationEntity);
          }

          dRepInfo.setStatus(
              dRepRegistrationEntity.getType().equals(DRepActionType.UNREG_DREP_CERT)
                  ? DRepStatus.INACTIVE
                  : DRepStatus.ACTIVE);
        });

    dRepInfoRepository.saveAll(dRepInfoMap.values());
  }
}
