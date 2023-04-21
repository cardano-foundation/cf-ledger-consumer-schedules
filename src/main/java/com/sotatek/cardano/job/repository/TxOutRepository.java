package com.sotatek.cardano.job.repository;

import com.sotatek.cardano.common.entity.TxOut;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TxOutRepository extends JpaRepository<TxOut, Long>{

  @Query("SELECT txo "
      + "FROM TxOut txo "
      + "WHERE txo.stakeAddress.id IS NOT NULL AND "
      + "txo.txId >= :minTxId AND "
      + "txo.txId <= :maxTxId AND "
      + "NOT EXISTS ( SELECT txo1.id FROM TxOut txo1 "
      + "INNER JOIN TxIn txi ON txi.txInputId = txo1.txId AND txo.index = txi.txOutIndex "
      + "WHERE txo1.id = txo.id )")
  List<TxOut> findTxOutsByRangeTxIdAndStakeId(Long minTxId, Long maxTxId);
}