package org.cardanofoundation.job.projection;

import java.math.BigInteger;

public interface EpochRewardProjection {

  Integer getEpochNo();

  BigInteger getAmount();
}
