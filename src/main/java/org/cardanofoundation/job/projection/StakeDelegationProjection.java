package org.cardanofoundation.job.projection;

import java.math.BigInteger;
import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public interface StakeDelegationProjection {
  String getTxHash();

  BigInteger getOutSum();

  BigInteger getFee();

  Timestamp getTime();

  Integer getEpochNo();

  String getPoolId();

  String getPoolName();
}
