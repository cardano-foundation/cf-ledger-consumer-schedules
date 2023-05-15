package org.cardanofoundation.job.service.interfaces;

public interface KafkaService {

  void sendMessage(String topic, String key, Object message);
}
