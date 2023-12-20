package org.cardanofoundation.job.mapper;

import org.cardanofoundation.job.dto.PoolCertificateHistory;
import org.cardanofoundation.job.projection.PoolCertificateProjection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PoolCertificateMapper {

  PoolCertificateHistory fromPoolCertificateProjection(PoolCertificateProjection poolCertificateProjection);

}