package org.cardanofoundation.job.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.cardanofoundation.explorer.consumercommon.entity.MultiAsset;

public interface MultiAssetRepository extends JpaRepository<MultiAsset, Long> {}
