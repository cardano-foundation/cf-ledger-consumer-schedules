package org.cardanofoundation.job.common.enumeration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public enum EventType {
  DELEGATION("Delegation"),
  DE_REGISTRATION("Deregistration"),
  REGISTRATION("Registration"),
  REWARDS("Rewards"),
  WITHDRAWAL("Withdrawal"),
  POOL_UPDATE("Pool Update"),
  POOL_SIZE("Pool Size");
  String value;
}
