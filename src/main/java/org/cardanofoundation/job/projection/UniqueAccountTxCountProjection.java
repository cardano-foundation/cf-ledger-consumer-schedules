package org.cardanofoundation.job.projection;

public interface UniqueAccountTxCountProjection {

  String getAccount();

  Integer getTxCount();
}
