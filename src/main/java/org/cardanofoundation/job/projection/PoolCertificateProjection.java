package org.cardanofoundation.job.projection;

import java.sql.Timestamp;

public interface PoolCertificateProjection {

  Long getTxId();

  String getTxHash();

  Integer getTxEpochNo();

  Integer getCertEpochNo();

  Integer getCertIndex();

  Long getPoolRetireId();

  Long getPoolUpdateId();

  Timestamp getBlockTime();

  Long getBlockNo();

  Integer getEpochSlotNo();

  Integer getSlotNo();
}
