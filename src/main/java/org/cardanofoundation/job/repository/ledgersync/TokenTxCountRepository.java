package org.cardanofoundation.job.repository.ledgersync;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount;

public interface TokenTxCountRepository extends JpaRepository<TokenTxCount, String> {

  List<TokenTxCount> findAllByUnitIn(@Param("units") Collection<String> units);
}
