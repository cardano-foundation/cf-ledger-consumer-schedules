package org.cardanofoundation.job.model.projection;

import java.math.BigInteger;

public interface TokenVolume {
  Long getIdent();

  BigInteger getVolume();
}
