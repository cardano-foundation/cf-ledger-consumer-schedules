package org.cardanofoundation.job.model;

import java.math.BigInteger;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TokenVolume {
  Long ident;
  BigInteger volume;

  public static TokenVolume from(org.cardanofoundation.job.model.projection.TokenVolume tokenVolume) {
    return new TokenVolume(tokenVolume.getIdent(), tokenVolume.getVolume());
  }

}
