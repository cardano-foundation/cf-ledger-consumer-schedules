package org.cardanofoundation.job.projection;

public interface PoolInfoProjection {

  Long getId();

  String getPoolId();

  String getPoolView();

  String getPoolName();
}
