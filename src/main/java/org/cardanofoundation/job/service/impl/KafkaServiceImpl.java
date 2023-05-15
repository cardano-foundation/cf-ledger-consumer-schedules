package org.cardanofoundation.job.service.impl;

import java.util.concurrent.CompletableFuture;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import org.cardanofoundation.job.service.interfaces.KafkaService;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class KafkaServiceImpl implements KafkaService {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Override
  public void sendMessage(String topic, String key, Object message) {
    CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);

    future.whenComplete((result, ex) -> {
      if (ex != null) {
        log.error("Send message fail {}", ex.getMessage());
        System.exit(0);
      } else {
        // Do logs in here
      }
    });
  }
}
