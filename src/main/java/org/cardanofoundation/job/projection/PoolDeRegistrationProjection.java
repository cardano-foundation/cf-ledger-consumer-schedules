package org.cardanofoundation.job.projection;

import java.math.BigInteger;
import java.sql.Timestamp;

public interface PoolDeRegistrationProjection {

  BigInteger getFee();

  String getTxHash();

  Long getTxId();

  String getPoolId();

  Integer getRetiringEpoch();

  Timestamp getTime();
}
