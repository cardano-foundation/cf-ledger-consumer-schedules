package org.cardanofoundation.job.repository.ledgersync;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.compositeKey.GovActionProposalId;
import org.cardanofoundation.explorer.common.entity.enumeration.GovActionType;
import org.cardanofoundation.explorer.common.entity.ledgersync.GovActionProposalInfo;

public interface GovActionProposalInfoRepository
    extends JpaRepository<GovActionProposalInfo, GovActionProposalId> {
  @Query(
      value =
          """
      SELECT gapi
      FROM GovActionProposalInfo gapi
      LEFT JOIN Tx tx on tx.hash = gapi.txHash
      WHERE gapi.type = :type
""")
  Slice<GovActionProposalInfo> findByType(@Param("type") GovActionType type, Pageable pageable);
}
