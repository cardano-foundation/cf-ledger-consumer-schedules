package org.cardanofoundation.job.repository.explorer;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.consumercommon.explorer.entity.SmartContractInfo;

public interface SmartContractInfoRepository extends JpaRepository<SmartContractInfo, Long> {

  List<SmartContractInfo> findAllByScriptHashIn(@Param("scriptHashes") Collection<String> scriptHashes);
}
