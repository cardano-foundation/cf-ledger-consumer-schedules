package org.cardanofoundation.job.model;


import java.math.BigInteger;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenVolume {
  Long ident;
  BigInteger volume;
}
