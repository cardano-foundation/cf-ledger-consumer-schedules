package com.sotatek.cardano.job.listeners;


import com.sotatek.cardano.job.event.KafkaRegistryEvent;
import com.sotatek.cardano.job.service.impl.CardanoCliServiceImpl;
import com.sotatek.cardano.job.service.interfaces.EpochStakeService;
import com.sotatek.cardano.job.service.interfaces.LedgerStateReader;
import com.sotatek.cardano.ledgersync.common.commands.EpochStakeCommand;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
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
import org.springframework.transaction.annotation.Transactional;
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
  @Value("${kafka.topics.epochStake.name}")
  private String kafkaTopicId;

  @KafkaListener(topics = "${kafka.topics.epochStake.name}", id = "${kafka.topics.epochStake.name}")
  public void listenCommandInsertEpochStake(ConsumerRecord<String, ExecuteCommand> consumerRecord,
      Acknowledgment ack) {
    EpochStakeCommand command = (EpochStakeCommand) consumerRecord.value();
      try {
        Map<String, Object> ledgerState = ledgerStateReader.readFile(
            CardanoCliServiceImpl.getFileName(networkMagic, "58"));

        if (!ObjectUtils.isEmpty(ledgerState)) {
          epochStakeService.handleLedgerState(ledgerState);
          ack.acknowledge();
          return;
        }

        ack.nack(Duration.ofSeconds(BigInteger.TEN.longValue()));
        publisher.publishEvent(new KafkaRegistryEvent(kafkaTopicId, Boolean.FALSE));
      } catch (Exception e) {
        log.error(e.getMessage());
        ack.nack(Duration.ofSeconds(BigInteger.TEN.longValue()));
        System.exit(0);
      }
  }
}
