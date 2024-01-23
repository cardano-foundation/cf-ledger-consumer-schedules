package org.cardanofoundation.job.projection;

public interface ScriptNumberHolderProjection {
  String getScriptHash();

  Long getNumberOfHolders();
}
