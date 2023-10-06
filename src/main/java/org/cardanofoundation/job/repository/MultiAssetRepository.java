package org.cardanofoundation.job.repository;

import java.util.List;
import java.sql.Timestamp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.MultiAsset;

@Repository
public interface MultiAssetRepository extends JpaRepository<MultiAsset, Long> {

  @Query("select distinct multiAsset "
      + " from MultiAsset multiAsset join AddressToken addressToken"
      + " on multiAsset.id = addressToken.multiAssetId"
      + " join Tx tx on tx.id = addressToken.txId"
      + " join Block block on block.id = tx.blockId"
      + " where block.blockNo > :fromBlockNo and block.blockNo <= :toBlockNo ")
  List<MultiAsset> getTokensInTransactionInBlockRange(@Param("fromBlockNo") Long fromBlockNo,
                                                      @Param("toBlockNo") Long toBlockNo);

  @Query("select multiAsset from MultiAsset multiAsset where multiAsset.time >= :time"
      + " and multiAsset.txCount = 0")
  List<MultiAsset> getTokensWithZeroTxCountAndAfterTime(@Param("time") Timestamp afterTime);

  @Query("select distinct multiAsset "
      + " from MultiAsset multiAsset join AddressToken addressToken"
      + " on multiAsset.id = addressToken.multiAssetId"
      + " join Tx tx on tx.id = addressToken.txId"
      + " join Block block on block.id = tx.blockId"
      + " where block.time >= :fromTime and block.time <= :toTime")
  List<MultiAsset> getTokensInTransactionInTimeRange(@Param("fromTime") Timestamp fromTime,
                                                     @Param("toTime") Timestamp toTime);


}
