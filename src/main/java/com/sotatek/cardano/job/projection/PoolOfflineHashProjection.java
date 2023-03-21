package com.sotatek.cardano.job.projection;

public interface PoolOfflineHashProjection {
  Long getPoolId();
  Long getPoolRefId();
  String getHash();

}
