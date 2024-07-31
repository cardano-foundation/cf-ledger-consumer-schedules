package org.cardanofoundation.job.projection.gov;

import java.sql.Timestamp;

public interface OffChainVoteFetchErrorProjection {
  String getAnchorUrl();

  String getAnchorHash();

  String getFetchError();

  Timestamp getFetchTime();

  Integer getRetryCount();
}
