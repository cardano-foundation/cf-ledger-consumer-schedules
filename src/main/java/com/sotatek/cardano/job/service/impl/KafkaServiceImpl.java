package com.sotatek.cardano.job.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.sotatek.cardano.job.service.interfaces.KafkaService;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class KafkaServiceImpl implements KafkaService {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Override
  public void sendMessage(String topic, String key, Object message) {
    ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);

    future.addCallback(
        new ListenableFutureCallback<>() {
          @Override
          public void onFailure(Throwable ex) {
            log.error("Send message fail {}", ex.getMessage());
            System.exit(0);
          }

          @Override
          public void onSuccess(SendResult<String, Object> result) {
            // Do logs in here
          }
        });
  }
}
