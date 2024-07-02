package org.cardanofoundation.job.repository.ledgersyncagg.jooq;

import static org.jooq.impl.DSL.excluded;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.lateral;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.substring;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.trueCondition;
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
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.AddressTxAmount;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.Address_;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.LatestTokenBalance;
import org.cardanofoundation.explorer.common.entity.ledgersyncsagg.LatestTokenBalance_;
import org.cardanofoundation.explorer.common.utils.EntityUtil;

@Repository
@Slf4j
public class JOOQLatestTokenBalanceRepository {

  private final DSLContext dsl;
  private final EntityUtil addressBalanceEntity;
  private final EntityUtil addressTxAmountEntity;
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
    this.addressTxAmountEntity = new EntityUtil(schema, AddressTxAmount.class);
    this.transactionManager = platformTransactionManager;
  }

  public void createAllIndexes() {
    long startTime = System.currentTimeMillis();
    String index1 =
        "create index if not exists latest_token_balance_unit_idx on latest_token_balance (unit)";
    String index2 =
        "create index if not exists latest_token_balance_policy_idx on latest_token_balance (policy)";
    String index3 =
        "create index if not exists latest_token_balance_slot_idx on latest_token_balance (slot)";
    String index4 =
        "create index if not exists latest_token_balance_quantity_idx on latest_token_balance (quantity)";
    String index5 =
        "create index if not exists latest_token_balance_block_time_idx on latest_token_balance (block_time)";
    String index6 =
        "create index if not exists latest_token_balance_unit_quantity_idx on latest_token_balance (unit, quantity)";
    String index7 =
        "create index if not exists latest_token_balance_policy_quantity_idx on latest_token_balance (policy, quantity)";

    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactionTemplate.execute(
        status -> {
          dsl.batch(index1, index2, index3, index4, index5, index6, index7).execute();
          return true;
        });

    log.info(
        "Created all indexes for latest token balance in {} ms",
        System.currentTimeMillis() - startTime);
  }

  public void dropAllIndexes() {
    long startTime = System.currentTimeMillis();
    log.info("Dropping all indexes for latest token balance");
    String index1 = "drop index if exists latest_token_balance_unit_idx";
    String index2 = "drop index if exists latest_token_balance_policy_idx";
    String index3 = "drop index if exists latest_token_balance_slot_idx";
    String index4 = "drop index if exists latest_token_balance_quantity_idx";
    String index5 = "drop index if exists latest_token_balance_block_time_idx";
    String index6 = "drop index if exists latest_token_balance_unit_quantity_idx";
    String index7 = "drop index if exists latest_token_balance_policy_quantity_idx";

    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactionTemplate.execute(
        status -> {
          dsl.batch(index1, index2, index3, index4, index5, index6, index7).execute();
          return true;
        });
    log.info(
        "Dropped all indexes for latest token balance in {} ms",
        System.currentTimeMillis() - startTime);
  }

  public void deleteAllZeroHolders() {
    log.info("Deleting all zero holders from latest token balance");
    long startTime = System.currentTimeMillis();
    var query =
        dsl.deleteFrom(table(latestTokenBalanceEntity.getTableName()))
            .where(
                field(latestTokenBalanceEntity.getColumnField(LatestTokenBalance_.QUANTITY)).eq(0));
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    int totalDeleted = transactionTemplate.execute(status -> query.execute());
    log.info(
        "Deleted all zero holders from latest token balance: {} record deleted in {} ms",
        totalDeleted,
        System.currentTimeMillis() - startTime);
  }

  public void insertLatestTokenBalanceByUnitIn(
      List<String> units, Long blockTimeCheckpoint, boolean includeZeroHolders) {
    long startTime = System.currentTimeMillis();
    var query =
        dsl.insertInto(table(latestTokenBalanceEntity.getTableName()))
            .select(
                with("address_unit_distinct")
                    .as(
                        select(
                                field(
                                    addressTxAmountEntity.getColumnField(AddressBalance_.ADDRESS)),
                                field(addressTxAmountEntity.getColumnField(AddressBalance_.UNIT)))
                            .from(table(addressTxAmountEntity.getTableName()))
                            .where(
                                field(addressTxAmountEntity.getColumnField(AddressBalance_.UNIT))
                                    .in(units),
                                field("block_time").gt(blockTimeCheckpoint))
                            .groupBy(
                                field(
                                    addressTxAmountEntity.getColumnField(AddressBalance_.ADDRESS)),
                                field(addressTxAmountEntity.getColumnField(AddressBalance_.UNIT))))
                    .select(
                        field("address_unit_distinct.address"),
                        field(addressEntity.getColumnField(Address_.STAKE_ADDRESS)),
                        substring(field("address_unit_distinct.unit").cast(String.class), 1, 56)
                            .as("policy"),
                        field("token_holder_balance.slot"),
                        field("address_unit_distinct.unit"),
                        field("token_holder_balance.quantity"),
                        field("token_holder_balance.block_time"))
                    .from(
                        table("address_unit_distinct")
                            .join(table(addressEntity.getTableName()).as("addr"))
                            .on(field("address_unit_distinct.address").eq(field("addr.address"))),
                        lateral(
                                select(
                                        field(
                                            addressBalanceEntity.getColumnField(
                                                AddressBalance_.SLOT)),
                                        field(
                                            addressBalanceEntity.getColumnField(
                                                AddressBalance_.QUANTITY)),
                                        field("block_time"))
                                    .from(
                                        table(addressBalanceEntity.getTableName())
                                            .where(
                                                field(
                                                        addressBalanceEntity.getColumnField(
                                                            AddressBalance_.ADDRESS))
                                                    .eq(
                                                        field(
                                                            "address_unit_distinct.address",
                                                            addressBalanceEntity.getColumnField(
                                                                AddressBalance_.ADDRESS))),
                                                field(
                                                        addressBalanceEntity.getColumnField(
                                                            AddressBalance_.UNIT))
                                                    .eq(
                                                        field(
                                                            "address_unit_distinct.unit",
                                                            addressBalanceEntity.getColumnField(
                                                                AddressBalance_.UNIT))),
                                                field("block_time").gt(blockTimeCheckpoint),
                                                includeZeroHolders
                                                    ? trueCondition()
                                                    : field(
                                                            addressBalanceEntity.getColumnField(
                                                                AddressBalance_.QUANTITY))
                                                        .gt(0)))
                                    .orderBy(
                                        field(
                                                addressBalanceEntity.getColumnField(
                                                    AddressBalance_.SLOT))
                                            .desc())
                                    .limit(1))
                            .as("token_holder_balance")))
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
        "Inserted latest token balance for {} units in {} ms",
        units.size(),
        System.currentTimeMillis() - startTime);
  }
}
