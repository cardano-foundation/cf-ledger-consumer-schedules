package com.sotatek.cardano.job.schedules;

import com.sotatek.cardano.job.cli.QueryCli;
import com.sotatek.cardano.job.cli.Tip;
import com.sotatek.cardano.job.dto.NodeInfo;
import com.sotatek.cardano.job.event.KafkaRegistryEvent;
import com.sotatek.cardano.job.service.interfaces.CardanoCliService;
import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Slf4j
@Component
public class CardanoCliSchedule {

  public static final String BYRON = "byron";
  private final QueryCli queryCli;
  private final CardanoCliService cardanoCliService;
  private final ApplicationEventPublisher publisher;
  private String epochStakeTopicId;
  public static final AtomicInteger  failEpoch = new AtomicInteger(0);

  public CardanoCliSchedule(QueryCli queryCli,
      CardanoCliService cardanoCliService,
      ApplicationEventPublisher publisher,
      @Value("${kafka.topics.epoch.name}") String epochStakeTopicId) {
    this.queryCli = queryCli;
    this.cardanoCliService = cardanoCliService;
    this.publisher = publisher;
    this.epochStakeTopicId = epochStakeTopicId;
  }


  @Scheduled(initialDelay = 300, fixedDelayString = "${jobs.cardano-cli.delay}")
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

      if (nodeInfo.getEpochNo() != BigInteger.ZERO.intValue() &&
          tip.getEpoch() - nodeInfo.getEpochNo() > BigInteger.ONE.longValue()) {
        cardanoCliService.removeContainer();
        return;
      }

      if (!tip.getEpoch().equals(nodeInfo.getEpochNo())) {
        log.info("current epoch {}, era {}", tip.getEpoch(), tip.getEra());
        var streamLedgerStateResult = cardanoCliService.runExec(queryCli.getLedgerState());
        if (Objects.nonNull(streamLedgerStateResult)) {
          cardanoCliService.writeLedgerState(streamLedgerStateResult, tip.getEpoch());

          if (failEpoch.get() != BigInteger.ZERO.intValue() && failEpoch.get() <= tip.getEpoch()) {
            publisher.publishEvent(new KafkaRegistryEvent(epochStakeTopicId, Boolean.TRUE));
            failEpoch.set(BigInteger.ZERO.intValue());
          }
        }
        nodeInfo.setEpochNo(tip.getEpoch());
        cardanoCliService.saveNodeInfo();
      }

    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }
}
