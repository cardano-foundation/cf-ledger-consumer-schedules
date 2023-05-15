package org.cardanofoundation.job.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.Delegation;


@Repository
public interface DelegationRepository extends JpaRepository<Delegation, Long> {

  @Query("SELECT d FROM Delegation d WHERE d.tx.id >= :minTxId AND d.tx.id <= :maxTxId")
  List<Delegation> getDelegationsByRangeTxId(Long minTxId, Long maxTxId);
}
