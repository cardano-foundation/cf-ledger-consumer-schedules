package org.cardanofoundation.job.common.enumeration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public enum StakeTxType {
  SENT("ADA sent from wallet"),
  RECEIVED("ADA received"),
  FEE_PAID("Transaction fee paid"),
  CERTIFICATE_FEE_PAID("Certificate fee paid"),
  CERTIFICATE_HOLD_PAID("Certificate hold paid"),
  CERTIFICATE_HOLD_DEPOSIT_REFUNDED("Certificate hold deposit refunded"),
  REWARD_WITHDRAWN("Reward withdrawn"),
  REWARD_WITHDRAWN_AND_CERTIFICATE_HOLD_PAID("Reward withdrawn and certificate hold paid"),
  REWARD_WITHDRAWN_AND_CERTIFICATE_HOLD_DEPOSIT_REFUNDED(
      "Reward withdrawn and certificate hold deposit refunded"),
  UNKNOWN("Unknown");
  String value;
}
