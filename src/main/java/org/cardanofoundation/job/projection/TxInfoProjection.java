package org.cardanofoundation.job.projection;

import java.sql.Timestamp;

public interface TxInfoProjection {
  Long getTxId();

  Timestamp getBlockTime();
}
