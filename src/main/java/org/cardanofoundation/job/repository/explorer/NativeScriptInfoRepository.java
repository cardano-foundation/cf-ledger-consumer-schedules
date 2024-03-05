package org.cardanofoundation.job.repository.explorer;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.explorer.NativeScriptInfo;

public interface NativeScriptInfoRepository extends JpaRepository<NativeScriptInfo, Long> {
  List<NativeScriptInfo> findAllByScriptHashIn(
      @Param("scriptHashes") Collection<String> scriptHashes);
}
