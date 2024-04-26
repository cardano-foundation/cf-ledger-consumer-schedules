package org.cardanofoundation.job.repository.explorer;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.common.entity.explorer.DRepInfo;

public interface DRepInfoRepository extends JpaRepository<DRepInfo, Long> {

  List<DRepInfo> findAllByDrepHashIn(Collection<String> drepHashes);
}
