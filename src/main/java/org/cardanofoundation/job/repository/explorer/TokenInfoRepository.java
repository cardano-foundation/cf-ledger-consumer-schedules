package org.cardanofoundation.job.repository.explorer;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.common.entity.explorer.TokenInfo;

@Repository
public interface TokenInfoRepository extends JpaRepository<TokenInfo, Long> {
  List<TokenInfo> findByUnitIn(@Param("multiAssetIds") Collection<String> units);
}
