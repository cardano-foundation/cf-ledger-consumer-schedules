package org.cardanofoundation.job.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PoolCountProjectionImpl implements PoolCountProjection {

  private Long poolId;
  private String poolView;
  private Integer countValue;
}
