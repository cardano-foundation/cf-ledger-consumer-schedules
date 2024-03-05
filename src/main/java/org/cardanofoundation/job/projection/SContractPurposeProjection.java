package org.cardanofoundation.job.projection;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import org.cardanofoundation.explorer.common.entity.enumeration.ScriptPurposeType;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class SContractPurposeProjection {

  String scriptHash;
  ScriptPurposeType scriptPurposeType;
}
