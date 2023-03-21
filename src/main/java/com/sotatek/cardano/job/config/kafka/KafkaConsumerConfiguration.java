package com.sotatek.cardano.job.config.kafka;

//@Configuration //TODO will uncomment when phase two to receive message to insert

//@Configuration
//@EnableKafka
public class KafkaConsumerConfiguration {

//  @Value("${spring.kafka.bootstrap-servers}")
//  private String bootstrapServers;
//
//  @Value("${spring.kafka.consumer.group-id}")
//  private String groupId;
//
//  @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages}")
//  private String trustedPackage;
//
//  @Bean
//  public ConsumerFactory<String, Object> consumerFactory() {
//    Map<String, Object> props = new HashMap<>();
//
//    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
//        bootstrapServers);
//    props.put(ConsumerConfig.GROUP_ID_CONFIG,
//        groupId);
//    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
//        StringDeserializer.class);
//    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
//        JsonDeserializer.class);
//    props.put(JsonDeserializer.TRUSTED_PACKAGES, trustedPackage);
//
//    return new DefaultKafkaConsumerFactory<>(props);
//  }
//
//  @Bean
//  public KafkaListenerContainerFactory<
//      ConcurrentMessageListenerContainer<String, Object>>
//  kafkaListenerContainerFactory(ConsumerFactory<String, Object> consumerFactory) {
//    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
//        new ConcurrentKafkaListenerContainerFactory<>();
//    factory.setConsumerFactory(consumerFactory);
//    return factory;
//  }
}