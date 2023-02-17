package com.sotatek.cardano.job.repository;

import com.sotatek.cardano.common.entity.MultiAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MultiAssetRepository extends JpaRepository<MultiAsset, Long> {

    @Query("SELECT ma FROM MultiAsset ma WHERE ma.policy IN :policyList AND ma.name IN :nameList")
    List<MultiAsset> findAllByPolicyAndName(List<String> policyList, List<String> nameList);
}
