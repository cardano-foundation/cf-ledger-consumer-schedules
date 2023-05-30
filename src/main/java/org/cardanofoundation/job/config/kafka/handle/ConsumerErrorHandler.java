package org.cardanofoundation.job.config.kafka.handle;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler;
import org.springframework.kafka.support.KafkaHeaders;

import org.apache.kafka.common.TopicPartition;

@Configuration
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsumerErrorHandler {

  @Bean
  public ConsumerAwareListenerErrorHandler orderListenerErrorHandler() {
    return (message, exception, consumer) -> {
      var headers = message.getHeaders();

      var currentReceivedPartitionId =
          headers.get(KafkaHeaders.RECEIVED_PARTITION, Integer.class);

      var currentOffset = headers.get(KafkaHeaders.RECEIVED_PARTITION, Long.class);

      var receivedTopic = headers.get(KafkaHeaders.RECEIVED_TOPIC, String.class);

      var exceptionMessageFormat =
          MessageFormat.format(
              "Kafka Listener error occurred : error message = {}", exception.getMessage());

      log.error(exceptionMessageFormat);

      if (Objects.isNull(currentReceivedPartitionId)) {
        return Optional.empty();
      }

      if (Objects.isNull(currentOffset)) {
        return Optional.empty();
      }

      var topicPartition = new TopicPartition(receivedTopic, currentReceivedPartitionId);

      consumer.seek(topicPartition, ++currentOffset);

      return Optional.empty();
    };
  }
}
