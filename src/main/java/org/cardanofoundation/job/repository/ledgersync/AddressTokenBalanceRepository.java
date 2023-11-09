package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.consumercommon.entity.AddressTokenBalance;
import org.cardanofoundation.job.projection.TokenNumberHoldersProjection;

public interface AddressTokenBalanceRepository extends JpaRepository<AddressTokenBalance, Long> {

  @Query(
      "SELECT COUNT(atb.addressId) as numberOfHolders, atb.multiAssetId as ident "
          + "FROM AddressTokenBalance atb "
          + "WHERE atb.multiAssetId IN :multiAssets "
          + "AND atb.stakeAddress.id IS NULL AND atb.balance > 0 "
          + "GROUP BY atb.multiAsset.id")
  List<TokenNumberHoldersProjection> countAddressNotHaveStakeByMultiAssetIn(
      @Param("multiAssets") List<Long> multiAssetIds);

  @Query(
      "SELECT COUNT(DISTINCT atb.stakeAddress.id) as numberOfHolders, atb.multiAssetId as ident "
          + "FROM AddressTokenBalance atb "
          + "WHERE atb.multiAssetId IN :multiAssets "
          + "AND atb.balance > 0 "
          + "GROUP BY atb.multiAsset.id")
  List<TokenNumberHoldersProjection> countByMultiAssetIn(
      @Param("multiAssets") List<Long> multiAssetIds);
}
