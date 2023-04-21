package com.sotatek.cardano.job.dto;

import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class PoolData{

  Long poolId;
  Long metadataRefId;
  String errorMessage;
  String hash;
  int status;
  byte[] json;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PoolData data = (PoolData) o;
    return poolId.equals(data.poolId) && metadataRefId.equals(data.metadataRefId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(poolId, metadataRefId);
  }
}



