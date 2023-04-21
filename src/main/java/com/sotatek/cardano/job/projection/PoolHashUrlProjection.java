package com.sotatek.cardano.job.projection;

public interface PoolHashUrlProjection {
  Long getPoolId();

  String getUrl();

  Long getMetadataId();
}
