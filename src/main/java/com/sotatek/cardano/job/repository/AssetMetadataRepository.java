package com.sotatek.cardano.job.repository;

import com.sotatek.cardano.common.entity.AssetMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetMetadataRepository extends JpaRepository<AssetMetadata, Long> {
}
