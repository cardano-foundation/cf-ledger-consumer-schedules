package org.cardanofoundation.job.event.listeners;

import java.util.Objects;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

import org.cardanofoundation.job.event.message.KafkaRegistryEvent;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class KafkaRegistryListener {

  KafkaListenerEndpointRegistry registry;

  @EventListener
  public void controlKafkaRegistry(KafkaRegistryEvent event) {
    Objects.requireNonNull(event.getListenerId());
    var listenerContainer = registry.getListenerContainer(event.getListenerId());
    Objects.requireNonNull(listenerContainer);

    if (event.isTurningOn()) {
      if (!listenerContainer.isRunning()) {
        listenerContainer.start();
      }
      return;
    }

    if (listenerContainer.isRunning()) {
      listenerContainer.stop();
    }
  }
}
