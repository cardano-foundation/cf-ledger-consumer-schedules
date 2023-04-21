package com.sotatek.cardano.job.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sotatek.cardano.common.entity.MultiAsset;

public interface MultiAssetRepository extends JpaRepository<MultiAsset, Long> {

  @Query("SELECT ma FROM MultiAsset ma WHERE ma.policy IN :policyList AND ma.name IN :nameList")
  List<MultiAsset> findAllByPolicyAndName(List<String> policyList, List<String> nameList);
}
