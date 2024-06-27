package org.cardanofoundation.job.repository.ledgersync.jooq;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;


import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTxAmount;
import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTxAmount_;
import org.cardanofoundation.explorer.common.entity.ledgersync.StakeAddressTxCount;
import org.cardanofoundation.explorer.common.entity.ledgersync.StakeAddressTxCount_;
import org.cardanofoundation.explorer.common.utils.EntityUtil;
import org.jooq.DSLContext;
import org.jooq.impl.SQLDataType;

import static org.jooq.impl.DSL.countDistinct;
import static org.jooq.impl.DSL.excluded;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;

@Repository
@Slf4j
public class JOOQStakeAddressTxCountRepository {

  private final DSLContext dsl;
  private final EntityUtil stakeAddressTxCountEntity;
  private final EntityUtil addressTxAmountEntity;
  private final PlatformTransactionManager transactionManager;

  public JOOQStakeAddressTxCountRepository(
      @Qualifier("ledgerSyncDSLContext") DSLContext dsl,
      @Value("${multi-datasource.datasourceLedgerSync.flyway.schemas}") String schema,
      @Qualifier("ledgerSyncTransactionManager") PlatformTransactionManager platformTransactionManager) {
    this.dsl = dsl;
    this.stakeAddressTxCountEntity = new EntityUtil(schema, StakeAddressTxCount.class);
    this.addressTxAmountEntity = new EntityUtil(schema, AddressTxAmount.class);
    this.transactionManager = platformTransactionManager;
  }

  public void insertStakeAddressTxCount(List<String> stakeAddresses) {
    long startTime = System.currentTimeMillis();
    var query =
        dsl.insertInto(table(stakeAddressTxCountEntity.getTableName()))
            .select(
                select(
                        field(addressTxAmountEntity.getColumnField(AddressTxAmount_.STAKE_ADDRESS))
                            .as("stake_address"),
                        countDistinct(
                                field(
                                    addressTxAmountEntity.getColumnField(AddressTxAmount_.TX_HASH)))
                            .cast(SQLDataType.NUMERIC)
                            .as("tx_count"))
                    .from(table(addressTxAmountEntity.getTableName()))
                    .where(
                        field(addressTxAmountEntity.getColumnField(AddressTxAmount_.STAKE_ADDRESS))
                            .in(stakeAddresses))
                    .groupBy(
                        field(
                            addressTxAmountEntity.getColumnField(AddressTxAmount_.STAKE_ADDRESS))))
            .onConflict(
                field(stakeAddressTxCountEntity.getColumnField(StakeAddressTxCount_.STAKE_ADDRESS)))
            .doUpdate()
            .set(
                field(stakeAddressTxCountEntity.getColumnField(StakeAddressTxCount_.TX_COUNT)),
                excluded(
                    field(
                        stakeAddressTxCountEntity.getColumnField(StakeAddressTxCount_.TX_COUNT),
                        SQLDataType.NUMERIC)));

    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactionTemplate.execute(
        status -> {
          query.execute();
          return true;
        });
    log.info(
        "Inserted stake address tx count for {} stake addresses in {} ms",
        stakeAddresses.size(),
        System.currentTimeMillis() - startTime);
  }
}
