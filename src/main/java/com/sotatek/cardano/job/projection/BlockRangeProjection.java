package com.sotatek.cardano.job.projection;

public interface BlockRangeProjection {
  Long getMinBlockId();

  Long getMaxBlockId();
}
