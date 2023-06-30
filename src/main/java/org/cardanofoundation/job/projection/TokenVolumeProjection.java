package org.cardanofoundation.job.projection;

import java.math.BigInteger;

public interface TokenVolumeProjection {
  Long getIdent();

  BigInteger getVolume();
}
