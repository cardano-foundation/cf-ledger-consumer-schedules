package org.cardanofoundation.job.projection;

public interface ScriptNumberTokenProjection {
  String getScriptHash();

  Long getNumberOfTokens();
}
