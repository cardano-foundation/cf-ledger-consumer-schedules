package org.cardanofoundation.job.event;

import org.springframework.context.ApplicationEvent;

import org.cardanofoundation.job.dto.PoolData;

public class FetchPoolDataSuccess extends ApplicationEvent {

  public FetchPoolDataSuccess(PoolData source) {
    super(source);
  }

  public PoolData getPoolData() {
    return PoolData.class.cast(source);
  }
}
