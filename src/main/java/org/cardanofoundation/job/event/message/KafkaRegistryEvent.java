package org.cardanofoundation.job.event.message;

import lombok.Getter;

import org.springframework.context.ApplicationEvent;

@Getter
public class KafkaRegistryEvent extends ApplicationEvent {

  private boolean turningOn;

  public KafkaRegistryEvent(String listenerId) {
    super(listenerId);
  }

  public KafkaRegistryEvent(String listenerId, boolean turningOn) {
    super(listenerId);
    this.turningOn = turningOn;
  }

  public String getListenerId() {
    return String.valueOf(this.getSource());
  }
}
