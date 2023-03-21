package com.sotatek.cardano.job.schedules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sotatek.cardano.job.cli.QueryCli;
import com.sotatek.cardano.job.cli.Tip;
import com.sotatek.cardano.job.constant.JobConstants;
import com.sotatek.cardano.job.dto.NodeInfo;
import com.sotatek.cardano.job.service.interfaces.CardanoCliService;
import com.sotatek.cardano.job.service.impl.CardanoCliServiceImpl;
import com.sotatek.cardano.job.service.interfaces.EpochParamService;
import com.sotatek.cardano.job.service.interfaces.EpochStakeService;
import com.sotatek.cardano.job.service.interfaces.RewardService;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Slf4j
@Component
public class CardanoCliSchedule {

  public static final String BYRON = "byron";
  public static final int CHARACTERS = 1024;
  public static final String SHELLEY = "Shelley";
  private final ObjectMapper mapper;
  private final QueryCli queryCli;
  private final CardanoCliService cardanoCliService;
  private final EpochStakeService epochStakeService;
  private final EpochParamService epochParamService;
  private final RewardService rewardService;
  private AtomicBoolean afterByronFirstTime = new AtomicBoolean(Boolean.TRUE);
  private String networkId;

  public CardanoCliSchedule(QueryCli queryCli, ObjectMapper mapper,
      CardanoCliService cardanoCliService, EpochStakeService epochStakeService,
      EpochParamService epochParamService, RewardService rewardService,
      @Value("${application.network-magic}") String networkId) {
    this.queryCli = queryCli;
    this.mapper = mapper;
    this.cardanoCliService = cardanoCliService;
    this.epochStakeService = epochStakeService;
    this.epochParamService = epochParamService;
    this.rewardService = rewardService;
    this.networkId = networkId;
  }


  @Scheduled(fixedDelayString = "${jobs.cardano-cli.delay}")
  public void getCliEpoch() {
    var streamEpochResult = cardanoCliService.runExec(queryCli.getTip());
    if (Objects.isNull(streamEpochResult)) {
      return;
    }

    try {
      Tip tip = cardanoCliService.readOutPutStream(streamEpochResult, Tip.class);

      if (ObjectUtils.isEmpty(tip.getEra()) ||
          tip.getEra().equalsIgnoreCase(BYRON) ||
          tip.getEpoch() == BigInteger.ZERO.intValue()) {
        return;
      }

      NodeInfo nodeInfo = cardanoCliService.getNodeInfo();

      if (afterByronFirstTime.get()
          && tip.getEra().equals(SHELLEY)) {
        afterByronFirstTime.set(Boolean.FALSE);
        nodeInfo.setEpochNo(tip.getEpoch());
        log.info("current epoch {}, era {}", tip.getEpoch(), tip.getEra());
        return;
      }

      if (tip.getEpoch() - nodeInfo.getEpochNo() > BigInteger.ONE.longValue()) {
        cardanoCliService.removeContainer();
        return;
      }

      if (Objects.nonNull(tip.getEpoch()) && !tip.getEpoch().equals(nodeInfo.getEpochNo())) {
        log.info("current epoch {}, era {}", tip.getEpoch(), tip.getEra());
        var streamLedgerStateResult = cardanoCliService.runExec(queryCli.getLedgerState());
        if (Objects.nonNull(streamLedgerStateResult)) {
          cardanoCliService.writeLedgerState(streamLedgerStateResult, tip.getEpoch());
          readFileAsync(String.valueOf(tip.getEpoch()));
        }
        nodeInfo.setEpochNo(tip.getEpoch());
        cardanoCliService.saveNodeInfo();
      }

    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

  private void readFile(String fileName) throws InterruptedException, IOException {
    File file = new File(fileName);
    if (file.exists()) {
      var loopTime = 0;

      while (!file.canRead()) {
        if (loopTime == BigInteger.TEN.intValue()) {
          return;
        }
        Thread.sleep(Duration.ofSeconds(BigInteger.TWO.longValue()).toMillis());
        loopTime = loopTime + 1;
      }

      StringBuilder builder = new StringBuilder();
      try (InputStreamReader inputStream = new InputStreamReader(new FileInputStream(file),
          StandardCharsets.UTF_8);) {
        char[] characters = new char[CHARACTERS];
        while (inputStream.ready()) {
          inputStream.read(characters);
          builder.append(characters);
        }
      } catch (Exception e) {
        log.error(e.getMessage());
      }

      LinkedHashMap<String, Object> ledgerState = mapper.readValue(builder.toString(),
          new TypeReference<>() {
          });

      epochStakeService.handleEpochStake(ledgerState);
      //TODO epochParamService.handleEpochParam();
      //TODO rewardService.handleReward();
    }
  }

  private void readFileAsync(String epochNo) {
    CompletableFuture.runAsync(() -> {
      try {
        readFile(String.join(JobConstants.DASH_DELIMITER, networkId,
            CardanoCliServiceImpl.FILE_NAME, epochNo));
      } catch (InterruptedException e) {
        log.error(e.getMessage());
      } catch (IOException e) {
        log.error(e.getMessage());
      }
    });
  }
}
