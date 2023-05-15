package org.cardanofoundation.job.projection;

public interface BlockRangeProjection {
  Long getMinBlockId();

  Long getMaxBlockId();
}
