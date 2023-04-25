package com.sotatek.cardano.job.projection;

public interface TxRangeProjection {
  Long getMinTxId();
  Long getMaxTxId();
}
