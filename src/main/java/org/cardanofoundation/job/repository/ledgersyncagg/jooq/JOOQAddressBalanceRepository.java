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

  public int cleanUpAddressBalanceV2(Long slotFrom, Long slotTo) {
    long startTime = System.currentTimeMillis();
    String deleteQuery =
        """
          WITH latest_balance AS
          ( SELECT DISTINCT
            ON (address, unit) address, unit, slot
            FROM address_balance
            WHERE slot between ? and ?
            ORDER BY address, unit, slot DESC)
          DELETE FROM address_balance ab
          USING latest_balance lb
          WHERE ab.address = lb.address
          AND ab.unit = lb.unit
          AND ab.slot >= ?
          AND ab.slot < lb.slot;
        """;

    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    int deletedRows =
        transactionTemplate.execute(status -> dsl.execute(deleteQuery, slotFrom, slotTo, slotFrom));
    log.info(
        "Cleaning address balance table completed in {} ms. Deleted {} rows",
        System.currentTimeMillis() - startTime,
        deletedRows);
    return deletedRows;
  }

  public void deleteAllZeroHolders(long targetSlot) {
    log.info("Deleting all zero holders from address balance");
    long startTime = System.currentTimeMillis();
    String deleteQuery =
        """
        DELETE
        FROM address_balance
        WHERE slot < ?
          AND unit != 'lovelace'
          AND quantity = 0;
        """;

    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    int deletedRows = transactionTemplate.execute(status -> dsl.execute(deleteQuery, targetSlot));
    log.info(
        "Deleted all zero holders from address balance: {} record deleted in {} ms",
        deletedRows,
        System.currentTimeMillis() - startTime);
  }
}
