package org.cardanofoundation.job.projection;

public interface TxRangeProjection {
  Long getMinTxId();

  Long getMaxTxId();
}
