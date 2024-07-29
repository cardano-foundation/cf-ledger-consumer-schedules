package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.compositeKey.OffChainVoteFetchErrorId;
import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteFetchError;

public interface OffChainVoteFetchErrorRepository
    extends JpaRepository<OffChainVoteFetchError, OffChainVoteFetchErrorId> {

  @Query(
      value =
          """
    SELECT e FROM OffChainVoteFetchError e WHERE e.retryCount <= :retryCount
    AND NOT EXISTS (SELECT 1 FROM OffChainVoteGovActionData d WHERE d.anchorUrl = e.anchorUrl AND d.anchorHash = e.anchorHash)
    """)
  List<OffChainVoteFetchError> findByRetryCountLessThanEqual(@Param("retryCount") int retryCount);
}
