package org.cardanofoundation.job.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TokenNumberHoldersProjectionImpl implements TokenNumberHoldersProjection {

  private Long ident;
  private Long numberOfHolders;
}