package org.cardanofoundation.job.repository.ledgersync;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTokenBalance;
import org.cardanofoundation.job.projection.ScriptNumberHolderProjection;

public interface AddressTokenBalanceRepository extends JpaRepository<AddressTokenBalance, Long> {

  @Query(
      "SELECT COALESCE(COUNT(DISTINCT(atb.addressId, atb.multiAssetId)), 0) as numberOfHolders, ma.policy as scriptHash"
          + " FROM AddressTokenBalance atb"
          + " INNER JOIN MultiAsset ma ON ma.id = atb.multiAssetId AND atb.balance > 0"
          + " WHERE ma.policy IN :policies"
          + " GROUP BY ma.policy")
  List<ScriptNumberHolderProjection> countHolderByPolicyIn(
      @Param("policies") Collection<String> policies);
}
