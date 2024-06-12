package org.cardanofoundation.job.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TokenUnitProjectionImpl implements TokenUnitProjection {

  Long ident;
  String unit;
}
