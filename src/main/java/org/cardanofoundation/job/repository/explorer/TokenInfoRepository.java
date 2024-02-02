package org.cardanofoundation.job.repository.explorer;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.explorer.entity.TokenInfo;

@Repository
public interface TokenInfoRepository extends JpaRepository<TokenInfo, Long> {

  List<TokenInfo> findByMultiAssetIdIn(Collection<Long> multiAssetIds);
}
