package org.cardanofoundation.job.projection;

public interface SContractTxCntProjection {

  String getScriptHash();

  Long getTxCount();
}
