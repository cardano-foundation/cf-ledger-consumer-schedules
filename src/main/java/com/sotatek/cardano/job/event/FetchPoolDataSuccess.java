package com.sotatek.cardano.job.event;

import org.springframework.context.ApplicationEvent;

import com.sotatek.cardano.job.dto.PoolData;

public class FetchPoolDataSuccess extends ApplicationEvent {

  public FetchPoolDataSuccess(PoolData source) {
    super(source);
  }

  public PoolData getPoolData() {
    return PoolData.class.cast(source);
  }
}
