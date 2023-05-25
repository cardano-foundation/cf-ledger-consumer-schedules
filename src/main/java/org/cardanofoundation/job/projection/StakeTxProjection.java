package org.cardanofoundation.job.projection;

import java.math.BigInteger;
import java.sql.Timestamp;

public interface StakeTxProjection {
  Long getTxId();
  BigInteger getAmount();

  Timestamp getTime();

}
