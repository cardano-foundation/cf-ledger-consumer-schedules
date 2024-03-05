package org.cardanofoundation.job.repository.ledgersync;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.enumeration.ScriptType;
import org.cardanofoundation.explorer.common.entity.ledgersync.Script;

public interface ScriptRepository extends JpaRepository<Script, Long> {

  Slice<Script> findAllByTypeIn(Collection<ScriptType> types, Pageable pageable);

  List<Script> findAllByHashIn(Collection<String> hashes);

  @Query(
      value =
          "SELECT s FROM Script s"
              + " WHERE s.hash IN (SELECT DISTINCT(r.scriptHash) FROM Redeemer r WHERE r.tx.id BETWEEN :fromTxId AND :toTxId)")
  Slice<Script> findAllByTxIn(
      @Param("fromTxId") Long fromTxId, @Param("toTxId") Long toTxId, Pageable pageable);

  @Query(
      "SELECT s.hash FROM Script s WHERE s.type IN :types AND s.tx.id BETWEEN :fromTxId AND :toTxId")
  Set<String> findHashByTypeAndTxIn(
      @Param("fromTxId") Long fromTxId,
      @Param("toTxId") Long toTxId,
      @Param("types") Collection<ScriptType> types);
}
