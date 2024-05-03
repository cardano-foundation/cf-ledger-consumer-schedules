package org.cardanofoundation.job.projection;

import java.math.BigInteger;
import java.sql.Timestamp;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class StakeTxProjection {

  Long txId;
  BigInteger amount;
  Long time;
}
