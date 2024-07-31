package org.cardanofoundation.job.projection.gov;

public interface GovActionProposalProjection {

  String getAnchorUrl();

  String getAnchorHash();

  Long getEpochNo();

  Long getBlockNo();
}
