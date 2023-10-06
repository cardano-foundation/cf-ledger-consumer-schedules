package org.cardanofoundation.job.projection;

import java.math.BigInteger;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class StakeTxBalanceProjection {

  Long txId;
  Long stakeAddressId;
  BigInteger balanceChange;
}
