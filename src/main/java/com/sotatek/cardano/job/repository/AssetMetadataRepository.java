package com.sotatek.cardano.job.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sotatek.cardano.common.entity.AssetMetadata;

public interface AssetMetadataRepository extends JpaRepository<AssetMetadata, Long> {}
