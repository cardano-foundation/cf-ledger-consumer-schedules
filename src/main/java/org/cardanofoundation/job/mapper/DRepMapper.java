package org.cardanofoundation.job.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import org.cardanofoundation.explorer.common.entity.explorer.DRepInfo;
import org.cardanofoundation.explorer.common.entity.ledgersync.DRepRegistrationEntity;

@Mapper(componentModel = "spring")
public interface DRepMapper {

  DRepInfo fromDRepRegistration(DRepRegistrationEntity dRepRegistration);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "drepHash", ignore = true)
  @Mapping(target = "drepId", ignore = true)
  void updateByDRepRegistration(
      @MappingTarget DRepInfo dRepInfo, DRepRegistrationEntity dRepRegistration);
}
