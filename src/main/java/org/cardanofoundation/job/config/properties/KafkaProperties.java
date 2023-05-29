package org.cardanofoundation.job.config.properties;

import java.util.HashMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConditionalOnProperty(value = "kafka.configuration-enabled", matchIfMissing = true, havingValue = "true")
@ConfigurationProperties(prefix = "kafka")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaProperties {

  Admin admin;

  Boolean autoCreateTopics;

  Map<String, TopicConfig> topics = new HashMap<>();

  Map<String, ProducerConfig> producers = new HashMap<>();

  Map<String, ConsumerConfig> consumers = new HashMap<>();

  @Data
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class TopicConfig {
    String name;
    Integer partitions;
    Short replicationFactor;
    Map<String, String> configs = new HashMap<>();
  }

  @Data
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class ProducerConfig {
    String clientId;
    String bootstrapServers;
    Integer maxInFlightRequestsPerConnection;
    Integer requestTimeoutMs;
    Integer batchSize;
    Integer lingerMs;
    Integer bufferMemory;
    String acks;
    String keySerializer;
    String valueSerializer;
    Integer retries;
    Boolean enableIdempotence;
  }

  @Data
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class ConsumerConfig {
    String bootstrapServers;
    String clientId;
    String keyDeserializer;
    String valueDeserializer;
    String autoOffsetReset;
    Boolean enableAutoCommit;
    Integer autoCommitIntervalMs;
    Integer sessionTimeoutMs;
    String trustedPackages;
    Boolean allowAutoCreateTopics;
    Integer concurrency;
    Integer pollTimeout;
    String groupId;
  }

  @Data
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class Admin {
    String bootstrapServers;
  }
}
