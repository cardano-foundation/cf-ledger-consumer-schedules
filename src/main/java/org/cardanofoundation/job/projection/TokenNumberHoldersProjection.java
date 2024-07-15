package org.cardanofoundation.job.projection;

public interface TokenNumberHoldersProjection {
  String getUnit();

  Long getNumberOfHolders();
}
