package org.cardanofoundation.job.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.TokenInfo;

@Repository
public interface TokenInfoRepository extends JpaRepository<TokenInfo, Long> {

  List<TokenInfo> findByFingerprintIn(Set<String> fingerprints);
}