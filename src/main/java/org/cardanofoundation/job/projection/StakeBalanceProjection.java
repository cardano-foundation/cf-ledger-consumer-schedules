package org.cardanofoundation.job.projection;

import java.math.BigInteger;

public interface StakeBalanceProjection {
  String getStakeAddress();

  BigInteger getBalance();
}
