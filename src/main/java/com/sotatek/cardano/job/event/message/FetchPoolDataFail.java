package com.sotatek.cardano.job.event.message;

import org.springframework.context.ApplicationEvent;

import com.sotatek.cardano.job.dto.PoolData;

public class FetchPoolDataFail extends ApplicationEvent {

  public FetchPoolDataFail(PoolData source) {
    super(source);
  }

  public PoolData getPoolData() {
    return PoolData.class.cast(source);
  }
}
