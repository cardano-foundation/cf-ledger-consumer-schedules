package org.cardanofoundation.job.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TokenNumberHolders {
  String unit;
  Long ident;
  Long numberOfHolders;

  public TokenNumberHolders(String unit, Long numberOfHolders) {
    this.unit = unit;
    this.numberOfHolders = numberOfHolders;
  }
}