package org.cardanofoundation.job.repository.explorer;

import org.cardanofoundation.explorer.consumercommon.explorer.entity.NativeScriptInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NativeScriptInfoRepository extends JpaRepository<NativeScriptInfo, Long> {
  List<NativeScriptInfo> findAllByScriptHashIn(@Param("scriptHashes") Collection<String> scriptHashes);
}
