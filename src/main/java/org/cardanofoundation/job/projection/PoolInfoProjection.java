package org.cardanofoundation.job.projection;

import java.math.BigInteger;

public interface PoolInfoProjection {

  Long getId();

  String getHashRaw();

  String getPoolView();

  String getPoolName();

  BigInteger getActiveStake();

  Long getPoolId();
}
