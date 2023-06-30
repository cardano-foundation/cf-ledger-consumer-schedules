package org.cardanofoundation.job.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.consumercommon.entity.AddressToken;
import org.cardanofoundation.explorer.consumercommon.entity.MultiAsset;
import org.cardanofoundation.job.projection.TokenVolumeProjection;

public interface AddressTokenRepository extends JpaRepository<AddressToken, Long> {

  @Query(
      value =
          "SELECT addrToken.multiAsset.id AS ident, "
              + " COALESCE(SUM(addrToken.balance), 0) AS volume"
              + " FROM AddressToken addrToken"
              + " WHERE addrToken.multiAsset IN :multiAsset "
              + " AND addrToken.balance > 0 AND addrToken.tx.id >= :txId"
              + " GROUP BY addrToken.multiAsset")
  List<TokenVolumeProjection> sumBalanceAfterTx(
      @Param("multiAsset") Collection<MultiAsset> multiAsset, @Param("txId") Long txId);
}
