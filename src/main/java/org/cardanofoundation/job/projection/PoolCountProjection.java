package org.cardanofoundation.job.projection;

public interface PoolCountProjection {

  Long getPoolId();

  String getPoolView();

  Integer getCountValue();
}
