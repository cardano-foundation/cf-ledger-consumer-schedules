package org.cardanofoundation.job.projection;

public interface PoolHashUrlProjection {
  Long getPoolId();
  String getUrl();
  Long getMetadataId();
}
