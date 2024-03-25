package org.cardanofoundation.job.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import org.cardanofoundation.explorer.common.entity.ledgersync.LatestVotingProcedure;
import org.cardanofoundation.explorer.common.entity.ledgersync.VotingProcedure;

@Mapper(componentModel = "spring")
public interface VotingProcedureMapper {

  LatestVotingProcedure fromVotingProcedure(VotingProcedure latestVotingProcedure);

  void updateByVotingProcedure(
      @MappingTarget LatestVotingProcedure latestVotingProcedure, VotingProcedure votingProcedure);
}
