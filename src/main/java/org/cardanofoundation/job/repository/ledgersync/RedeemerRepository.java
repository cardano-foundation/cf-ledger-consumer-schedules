package org.cardanofoundation.job.repository.ledgersync;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.consumercommon.entity.Redeemer;
import org.cardanofoundation.job.projection.SContractPurposeProjection;
import org.cardanofoundation.job.projection.SContractTxCntProjection;

public interface RedeemerRepository extends JpaRepository<Redeemer, Long> {


  @Query(
      value =
          "SELECT r.scriptHash as scriptHash, COUNT(DISTINCT(r.tx.id)) as txCount"
              + " FROM Redeemer r"
              + " WHERE r.scriptHash IN :scriptHashes"
              + " GROUP BY r.scriptHash")
  List<SContractTxCntProjection> getSContractTxCountByHashIn(
      @Param("scriptHashes") Collection<String> scriptHashes);

  @Query(
      value =
          "SELECT DISTINCT new org.cardanofoundation.job.projection.SContractPurposeProjection(r.scriptHash, r.purpose)"
              + " FROM Redeemer r"
              + " WHERE r.scriptHash IN :scriptHashes")
  List<SContractPurposeProjection> getScriptPurposeTypeByScriptHashIn(
      @Param("scriptHashes") Collection<String> scriptHashes);
}
