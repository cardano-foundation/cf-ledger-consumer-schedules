package org.cardanofoundation.job.repository.ledgersync.jooq;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import org.cardanofoundation.explorer.common.entity.ledgersync.Address;
import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTxAmount;
import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTxAmount_;
import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTxCount;
import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTxCount_;
import org.cardanofoundation.explorer.common.entity.ledgersync.Address_;
import org.cardanofoundation.explorer.common.utils.EntityUtil;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

import static org.jooq.impl.DSL.countDistinct;
import static org.jooq.impl.DSL.excluded;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;

@Repository
@Slf4j
public class JOOQAddressTxCountRepository {

  private final DSLContext dsl;
  private final EntityUtil addressTxCountEntity;
  private final EntityUtil addressTxAmountEntity;
  private final EntityUtil addressEntity;
  private final PlatformTransactionManager transactionManager;

  public JOOQAddressTxCountRepository(
      @Qualifier("ledgerSyncDSLContext") DSLContext dsl,
      @Value("${multi-datasource.datasourceLedgerSync.flyway.schemas}") String schema,
      @Qualifier("ledgerSyncTransactionManager") PlatformTransactionManager platformTransactionManager) {
    this.dsl = dsl;
    this.addressTxCountEntity = new EntityUtil(schema, AddressTxCount.class);
    this.addressTxAmountEntity = new EntityUtil(schema, AddressTxAmount.class);
    this.addressEntity = new EntityUtil(schema, Address.class);
    this.transactionManager = platformTransactionManager;
  }

  /**
   * Inserts or updates address transaction counts in the database for a given range.
   *
   * @param from Starting ID of the range (inclusive).
   * @param to Ending ID of the range (inclusive).
   * @param batchSize The size of each batch to be processed in one go.
   */
  public void insertAddressTxCount(long from, long to, long batchSize) {
    long currentFrom = from;

    // Loop through the range, processing in batches
    while (currentFrom <= to) {

      // Calculate the end value for the current batch
      long currentTo = Math.min(currentFrom + batchSize - 1, to);

      final String addressTxAmountAlias = "ata";
      final String addressAlias = "a";

      // SQL query for batch insert/update
      // Execute the batch insert/update query with the current range
      var query =
          dsl.insertInto(table(addressTxCountEntity.getTableName()))
              .select(
                  select(
                          field(
                                  name(
                                      addressTxAmountAlias,
                                      addressTxAmountEntity.getColumnField(
                                          AddressTxAmount_.ADDRESS)))
                              .as("address"),
                          countDistinct(
                                  field(
                                      addressTxAmountEntity.getColumnField(
                                          AddressTxAmount_.TX_HASH)))
                              .cast(SQLDataType.NUMERIC)
                              .as("tx_count"))
                      .from(table(addressTxAmountEntity.getTableName()).as(addressTxAmountAlias))
                      .innerJoin(table(addressEntity.getTableName()).as(addressAlias))
                      .on(
                          field(
                                  name(
                                      addressTxAmountAlias,
                                      addressTxAmountEntity.getColumnField(
                                          AddressTxAmount_.ADDRESS)))
                              .eq(
                                  field(
                                      name(
                                          addressAlias,
                                          addressEntity.getColumnField(Address_.ADDRESS)))))
                      .where(field(name(addressAlias, "id")).between(currentFrom).and(currentTo))
                      .groupBy(
                          field(
                              name(
                                  addressTxAmountAlias,
                                  addressTxAmountEntity.getColumnField(AddressTxAmount_.ADDRESS)))))
              .onConflict(
                  field(
                      name(
                          addressTxAmountAlias,
                          addressTxAmountEntity.getColumnField(AddressTxAmount_.ADDRESS))))
              .doUpdate()
              .set(
                  field(name("tx_count")),
                  excluded(
                      field(
                          addressTxCountEntity.getColumnField(AddressTxCount_.TX_COUNT),
                          SQLDataType.NUMERIC)));

      TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
      transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
      transactionTemplate.execute(
          status -> {
            query.execute();
            return true;
          });

      // Move to the next batch
      currentFrom = currentTo + 1;
    }
  }

  public void updateAddressTxCount(List<String> addressList) {
    Table<?> addressTxCountTable = table("address_tx_count");
    Table<?> addressTxAmountTable = table("address_tx_amount");

    Field<String> addressField = field(name("address"), String.class);
    Field<Integer> txCountField = field(name("tx_count"), SQLDataType.INTEGER);

    String addressTxAmountAlias = "ata";

    var query =
        dsl.insertInto(addressTxCountTable)
            .select(
                select(
                        field(
                                name(
                                    addressTxAmountAlias,
                                    addressTxAmountEntity.getColumnField(AddressTxAmount_.ADDRESS)))
                            .as("address"),
                        countDistinct(
                                field(
                                    addressTxAmountEntity.getColumnField(AddressTxAmount_.TX_HASH)))
                            .cast(SQLDataType.NUMERIC)
                            .as("tx_count"))
                    .from(addressTxAmountTable.as(addressTxAmountAlias))
                    .where(
                        field(
                                name(
                                    addressTxAmountAlias,
                                    addressTxAmountEntity.getColumnField(AddressTxAmount_.ADDRESS)))
                            .in(addressList))
                    .groupBy(
                        field(
                            name(
                                addressTxAmountAlias,
                                addressTxAmountEntity.getColumnField(AddressTxAmount_.ADDRESS)))))
            .onConflict(addressField)
            .doUpdate()
            .set(
                field(name("tx_count")),
                excluded(
                    field(
                        addressTxCountEntity.getColumnField(AddressTxCount_.TX_COUNT),
                        SQLDataType.NUMERIC)));

    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactionTemplate.execute(
        status -> {
          query.execute();
          return true;
        });
  }
}
