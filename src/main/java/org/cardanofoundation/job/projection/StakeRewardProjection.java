package org.cardanofoundation.job.projection;

import java.math.BigInteger;
import java.util.Date;

public interface StakeRewardProjection {

  Integer getEpoch();

  Date getTime();

  BigInteger getAmount();
}
