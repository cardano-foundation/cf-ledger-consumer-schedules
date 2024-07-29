package org.cardanofoundation.job.repository.ledgersync;

import java.util.Collection;
import java.util.List;

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

  List<Script> findAllByHashInAndTypeIn(Collection<String> hashes, Collection<ScriptType> types);

  @Query(
      value =
          "SELECT s FROM Script s"
              + " WHERE s.hash IN (SELECT DISTINCT(r.scriptHash) FROM Redeemer r WHERE r.tx.id BETWEEN :fromTxId AND :toTxId)")
  Slice<Script> findAllByTxIn(
      @Param("fromTxId") Long fromTxId, @Param("toTxId") Long toTxId, Pageable pageable);
}
