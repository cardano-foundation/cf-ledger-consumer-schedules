package org.cardanofoundation.job.projection;

import java.math.BigInteger;

public interface StakeAddressProjection {
  Long getId();

  Long getAddress();

  String getStakeAddress();

  BigInteger getTotalStake();
}
