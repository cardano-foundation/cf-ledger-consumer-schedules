package org.cardanofoundation.job.repository.ledgersyncagg.jooq;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import org.jooq.DSLContext;

@Repository
@Slf4j
public class JOOQAddressBalanceRepository {

  private final DSLContext dsl;
  private final PlatformTransactionManager transactionManager;

  public JOOQAddressBalanceRepository(
      @Qualifier("ledgerSyncAggDSLContext") DSLContext dsl,
      @Value("${multi-datasource.datasourceLedgerSyncAgg.flyway.schemas}") String schema,
      @Qualifier("ledgerSyncAggTransactionManager")
          PlatformTransactionManager platformTransactionManager) {
    this.dsl = dsl;
    this.transactionManager = platformTransactionManager;
  }

  public int cleanUpAddressBalance(long targetSlot, int batchSize) {
    log.info("Cleaning address balance table");
    long startTime = System.currentTimeMillis();
    String deleteQuery =
        """
        WITH rows_to_delete AS (SELECT address, unit, slot
                                FROM address_balance a1
                                WHERE a1.slot < ?
                                  AND EXISTS (SELECT 1
                                              FROM address_balance a2
                                              WHERE a1.address = a2.address
                                                AND a1.unit = a2.unit
                                                AND a1.slot < a2.slot)
                                LIMIT ?)
        DELETE
        FROM address_balance
        WHERE (address, unit, slot) IN (SELECT address, unit, slot FROM rows_to_delete);
        """;

    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    int deletedRows =
        transactionTemplate.execute(status -> dsl.execute(deleteQuery, targetSlot, batchSize));
    log.info(
        "Cleaning address balance table completed in {} ms. Deleted {} rows",
        System.currentTimeMillis() - startTime,
        deletedRows);
    return deletedRows;
  }
}
