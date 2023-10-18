package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.consumercommon.entity.AssetMetadata;

public interface AssetMetadataRepository extends JpaRepository<AssetMetadata, Long> {
  List<AssetMetadata> findBySubjectIn(@Param("subjects") Set<String> subjects);
}
