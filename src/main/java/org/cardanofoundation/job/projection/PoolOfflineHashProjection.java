package org.cardanofoundation.job.projection;

public interface PoolOfflineHashProjection {
  Long getPoolId();

  Long getPoolRefId();

  String getHash();
}
