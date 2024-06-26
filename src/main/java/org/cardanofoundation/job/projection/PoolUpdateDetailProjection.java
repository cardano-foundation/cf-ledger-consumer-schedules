package org.cardanofoundation.job.projection;

import java.math.BigInteger;
import java.sql.Timestamp;

public interface PoolUpdateDetailProjection {

  Long getPoolUpdateId();

  Long getHashId();

  String getPoolId();

  String getPoolName();

  String getPoolView();

  String getTxHash();

  Timestamp getTime();

  BigInteger getFee();

  String getRewardAccount();

  String getVrfKey();

  BigInteger getPledge();

  Double getMargin();

  BigInteger getCost();

  String getMetadataUrl();

  String getMetadataHash();

  BigInteger getDeposit();
}
