package org.cardanofoundation.job.repository.ledgersync;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.cardanofoundation.explorer.common.entity.compositeKey.AddressTxAmountId;
import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTxAmount;
import org.cardanofoundation.job.projection.UniqueAccountTxCountProjection;

public interface AddressTxAmountRepository
    extends JpaRepository<AddressTxAmount, AddressTxAmountId> {

  @Query(
      """
      select (case when ata.stakeAddress is null then ata.address else ata.stakeAddress end) as account, count(distinct(ata.txHash)) as txCount
      from AddressTxAmount ata
      where ata.epoch = :epochNo
      group by account
      """)
  List<UniqueAccountTxCountProjection> findUniqueAccountsInEpoch(@Param("epochNo") Integer epochNo);
}
