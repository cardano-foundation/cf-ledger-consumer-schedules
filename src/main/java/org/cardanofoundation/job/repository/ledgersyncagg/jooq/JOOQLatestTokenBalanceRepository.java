package org.cardanofoundation.job.repository.ledgersyncagg.jooq;

import static org.jooq.impl.DSL.excluded;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.substring;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.with;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import org.jooq.DSLContext;
import org.jooq.impl.SQLDataType;

import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.Address;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.AddressBalance;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.AddressBalance_;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.Address_;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.LatestTokenBalance;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.LatestTokenBalance_;
import org.cardanofoundation.explorer.common.utils.EntityUtil;

@Repository
@Slf4j
public class JOOQLatestTokenBalanceRepository {

  private final DSLContext dsl;
  private final EntityUtil addressBalanceEntity;
  private final EntityUtil latestTokenBalanceEntity;
  private final EntityUtil addressEntity;
  private final PlatformTransactionManager transactionManager;

  public JOOQLatestTokenBalanceRepository(
      @Qualifier("ledgerSyncAggDSLContext") DSLContext dsl,
      @Value("${multi-datasource.datasourceLedgerSyncAgg.flyway.schemas}") String schema,
      @Qualifier("ledgerSyncAggTransactionManager")
          PlatformTransactionManager platformTransactionManager) {
    this.dsl = dsl;
    this.addressBalanceEntity = new EntityUtil(schema, AddressBalance.class);
    this.latestTokenBalanceEntity = new EntityUtil(schema, LatestTokenBalance.class);
    this.addressEntity = new EntityUtil(schema, Address.class);
    this.transactionManager = platformTransactionManager;
  }

  public void insertLatestTokenBalanceByAddressIn(List<String> addresses) {
    long startTime = System.currentTimeMillis();
    var query =
        dsl.insertInto(table(latestTokenBalanceEntity.getTableName()))
            .select(
                with("full_balances")
                    .as(
                        select(
                                field(addressBalanceEntity.getColumnField(AddressBalance_.ADDRESS)),
                                field(addressBalanceEntity.getColumnField(AddressBalance_.UNIT)),
                                field(addressBalanceEntity.getColumnField(AddressBalance_.SLOT)),
                                field(
                                    addressBalanceEntity.getColumnField(AddressBalance_.QUANTITY)),
                                field("block_time"))
                            .distinctOn(
                                field(addressBalanceEntity.getColumnField(AddressBalance_.ADDRESS)),
                                field(addressBalanceEntity.getColumnField(AddressBalance_.UNIT)))
                            .from(table(addressBalanceEntity.getTableName()))
                            .where(
                                field(addressBalanceEntity.getColumnField(AddressBalance_.UNIT))
                                    .notEqual("lovelace"))
                            .orderBy(
                                field(addressBalanceEntity.getColumnField(AddressBalance_.ADDRESS)),
                                field(addressBalanceEntity.getColumnField(AddressBalance_.UNIT)),
                                field(addressBalanceEntity.getColumnField(AddressBalance_.SLOT))
                                    .desc()))
                    .select(
                        field("addr.address"),
                        field(addressEntity.getColumnField(Address_.STAKE_ADDRESS)),
                        substring(
                                field(addressBalanceEntity.getColumnField(AddressBalance_.UNIT))
                                    .cast(String.class),
                                1,
                                56)
                            .as("policy"),
                        field(addressBalanceEntity.getColumnField(AddressBalance_.SLOT)),
                        field(addressBalanceEntity.getColumnField(AddressBalance_.UNIT)),
                        field(addressBalanceEntity.getColumnField(AddressBalance_.QUANTITY)),
                        field("block_time"))
                    .from(
                        table("full_balances")
                            .join(table(addressEntity.getTableName()).as("addr"))
                            .on(field("addr.address").eq(field("full_balances.address")))
                            .where(
                                field("full_balances.address")
                                    .in(addresses)
                                    .and(field("full_balances.quantity").gt(0)))))
            .onConflict(
                field(latestTokenBalanceEntity.getColumnField(LatestTokenBalance_.ADDRESS)),
                field(latestTokenBalanceEntity.getColumnField(LatestTokenBalance_.UNIT)))
            .doUpdate()
            .set(
                field(latestTokenBalanceEntity.getColumnField(LatestTokenBalance_.QUANTITY)),
                excluded(
                    field(
                        latestTokenBalanceEntity.getColumnField(LatestTokenBalance_.QUANTITY),
                        SQLDataType.NUMERIC)))
            .set(
                field(latestTokenBalanceEntity.getColumnField(LatestTokenBalance_.SLOT)),
                excluded(
                    field(
                        latestTokenBalanceEntity.getColumnField(LatestTokenBalance_.SLOT),
                        SQLDataType.NUMERIC)))
            .set(
                field(latestTokenBalanceEntity.getColumnField(LatestTokenBalance_.BLOCK_TIME)),
                excluded(
                    field(
                        latestTokenBalanceEntity.getColumnField(LatestTokenBalance_.BLOCK_TIME),
                        SQLDataType.NUMERIC)));

    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactionTemplate.execute(
        status -> {
          query.execute();
          return true;
        });
    log.info(
        "Inserted latest token balance for {} addresses in {} ms",
        addresses.size(),
        System.currentTimeMillis() - startTime);
  }
}
