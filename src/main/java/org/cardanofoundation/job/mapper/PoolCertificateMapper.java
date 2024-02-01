package org.cardanofoundation.job.mapper;

import org.mapstruct.Mapper;

import org.cardanofoundation.job.dto.PoolCertificateHistory;
import org.cardanofoundation.job.projection.PoolCertificateProjection;

@Mapper(componentModel = "spring")
public interface PoolCertificateMapper {

  PoolCertificateHistory fromPoolCertificateProjection(
      PoolCertificateProjection poolCertificateProjection);
}
