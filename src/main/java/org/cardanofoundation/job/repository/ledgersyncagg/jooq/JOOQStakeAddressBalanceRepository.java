package org.cardanofoundation.job.repository.ledgersyncagg.jooq;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import org.jooq.DSLContext;

@Repository
@Slf4j
public class JOOQStakeAddressBalanceRepository {

  private final DSLContext dsl;
  private final PlatformTransactionManager transactionManager;

  public JOOQStakeAddressBalanceRepository(
      @Qualifier("ledgerSyncAggDSLContext") DSLContext dsl,
      @Qualifier("ledgerSyncAggTransactionManager")
          PlatformTransactionManager platformTransactionManager) {
    this.dsl = dsl;
    this.transactionManager = platformTransactionManager;
  }

  public int cleanUpStakeAddressBalance(long targetSlot, int batchSize) {
    long startTime = System.currentTimeMillis();
    String deleteQuery =
        """
        WITH rows_to_delete AS (SELECT address, slot
                                FROM stake_address_balance sab1
                                WHERE sab1.slot < ?
                                  AND EXISTS (SELECT 1
                                              FROM stake_address_balance sab2
                                              WHERE sab1.address = sab2.address
                                                AND sab1.slot < sab2.slot)
                                LIMIT ?)
        DELETE
        FROM stake_address_balance
        WHERE (address, slot) IN (SELECT address, slot FROM rows_to_delete);
        """;

    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    int deletedRows =
        transactionTemplate.execute(status -> dsl.execute(deleteQuery, targetSlot, batchSize));
    log.info(
        "Cleaning stake address balance table completed in {} ms. Deleted {} rows",
        System.currentTimeMillis() - startTime,
        deletedRows);
    return deletedRows;
  }
}
