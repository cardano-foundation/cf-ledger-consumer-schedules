package com.sotatek.cardano.job.listeners;


import com.sotatek.cardano.job.event.KafkaRegistryEvent;
import com.sotatek.cardano.job.schedules.CardanoCliSchedule;
import com.sotatek.cardano.job.service.impl.CardanoCliServiceImpl;
import com.sotatek.cardano.job.service.interfaces.EpochStakeService;
import com.sotatek.cardano.job.service.interfaces.LedgerStateReader;
import com.sotatek.cardano.ledgersync.common.commands.EpochStakeCommand;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import com.sotatek.cardano.ledgersync.common.commands.ExecuteCommand;
import org.springframework.util.ObjectUtils;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class EpochStakeListener {

  private final EpochStakeService epochStakeService;
  private final LedgerStateReader ledgerStateReader;
  private final ApplicationEventPublisher publisher;
  @Value("${application.network-magic}")
  private String networkMagic;
  @Value("${kafka.topics.epoch.name}")
  private String kafkaTopicId;

  @KafkaListener(topics = "${kafka.topics.epoch.name}",
      id = "${kafka.topics.epoch.name}",
      groupId = "${kafka.consumers.json-consumer.groupId}")
  public void listenCommandInsertEpochStake(ConsumerRecord<String, ExecuteCommand> consumerRecord,
      Acknowledgment ack) {
    EpochStakeCommand command = (EpochStakeCommand) consumerRecord.value();
    Integer maxEpoch = epochStakeService.findMaxEpochNoStaked();
    Integer epochMessage = Integer.parseInt(command.getExecuteEpoch());
    if (maxEpoch >= epochMessage) {
      ack.acknowledge();
      return;
    }
    // case update epoch stake table for new developed feature
    // will be remove in next commit
    if (maxEpoch == BigInteger.ZERO.intValue()) {
      IntStream.range(maxEpoch, epochMessage)
          .boxed()
          .map(String::valueOf)
          .forEach(epochNo -> {
            try {
              extractLedgerStateThenInsert(epochNo);
            } catch (Exception e) {
              log.error(e.getMessage());
            }
          });
    }

    insertMessage(ack, command.getExecuteEpoch());
  }

  private void insertMessage(Acknowledgment ack, String epochNo) {
    log.info("Insert epoch {}", epochNo);
    try {
      if (extractLedgerStateThenInsert(epochNo)) {
        ack.acknowledge();
        return;
      }

      ack.nack(Duration.ofSeconds(BigInteger.TEN.longValue()));
      CardanoCliSchedule.failEpoch.set(Integer.parseInt(epochNo));
      publisher.publishEvent(new KafkaRegistryEvent(kafkaTopicId, Boolean.FALSE));
    } catch (Exception e) {
      log.error(e.getMessage());
      ack.nack(Duration.ofSeconds(BigInteger.TEN.longValue()));
      System.exit(0);
    }
  }

  private boolean extractLedgerStateThenInsert(String epochNo)
      throws InterruptedException, IOException {
    Map<String, Object> ledgerState = ledgerStateReader.readFile(
        CardanoCliServiceImpl.getFileName(networkMagic, epochNo));

    if (!ObjectUtils.isEmpty(ledgerState)) {
      epochStakeService.handleLedgerState(ledgerState);
      return true;
    }
    return false;
  }
}
