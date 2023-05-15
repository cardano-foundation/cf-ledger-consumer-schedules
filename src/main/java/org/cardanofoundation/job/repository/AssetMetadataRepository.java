package org.cardanofoundation.job.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.consumercommon.entity.AssetMetadata;


public interface AssetMetadataRepository extends JpaRepository<AssetMetadata, Long> {}
