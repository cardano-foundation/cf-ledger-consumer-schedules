package com.sotatek.cardano.job.event;

import com.sotatek.cardano.job.dto.PoolData;
import org.springframework.context.ApplicationEvent;

public class FetchPoolDataFail extends ApplicationEvent {

  public FetchPoolDataFail(PoolData source) {
    super(source);
  }

  public PoolData getPoolData(){
    return PoolData.class.cast(source);
  }
}