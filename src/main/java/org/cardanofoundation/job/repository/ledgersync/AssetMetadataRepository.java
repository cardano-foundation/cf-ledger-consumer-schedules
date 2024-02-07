package org.cardanofoundation.job.repository.ledgersync;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.common.entity.ledgersync.AssetMetadata;

public interface AssetMetadataRepository extends JpaRepository<AssetMetadata, Long> {}
