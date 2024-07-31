package org.cardanofoundation.job.mapper;

import org.mapstruct.Mapper;

import org.cardanofoundation.job.dto.govActionMetaData.Anchor;
import org.cardanofoundation.job.projection.gov.AnchorProjection;

@Mapper(componentModel = "spring")
public interface AnchorMapper {
  Anchor fromAnchorProjection(AnchorProjection anchorProjection);
}
