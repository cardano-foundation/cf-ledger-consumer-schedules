package org.cardanofoundation.job.event.message;

import org.cardanofoundation.job.dto.PoolData;

import org.springframework.context.ApplicationEvent;

public class FetchPoolDataSuccess extends ApplicationEvent {

  public FetchPoolDataSuccess( PoolData source) {
    super(source);
  }

  public PoolData getPoolData(){
    return PoolData.class.cast(source);
  }
}
