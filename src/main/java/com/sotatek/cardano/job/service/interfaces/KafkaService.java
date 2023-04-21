package com.sotatek.cardano.job.service.interfaces;

public interface KafkaService {

  void sendMessage(String topic, String key, Object message);
}
