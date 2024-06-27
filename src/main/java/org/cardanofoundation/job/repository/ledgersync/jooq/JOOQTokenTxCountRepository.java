package org.cardanofoundation.job.repository.ledgersync.jooq;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import org.jooq.DSLContext;
import org.jooq.Query;

import org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount;
import org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount_;
import org.cardanofoundation.explorer.common.utils.EntityUtil;

@Repository
public class JOOQTokenTxCountRepository {

  private final DSLContext dsl;
  private final EntityUtil entityUtil;

  public JOOQTokenTxCountRepository(
      @Qualifier("ledgerSyncDSLContext") DSLContext dsl,
      @Value("${multi-datasource.datasourceLedgerSync.flyway.schemas}") String schema) {
    this.dsl = dsl;
    this.entityUtil = new EntityUtil(schema, TokenTxCount.class);
  }

  public void insertAll(List<TokenTxCount> tokenTxCounts) {
    if (tokenTxCounts.isEmpty()) {
      return;
    }

    List<Query> queries = new ArrayList<>();

    for (TokenTxCount tokenTxCount : tokenTxCounts) {
      var query =
          dsl.insertInto(table(entityUtil.getTableName()))
              .columns(
                  field(entityUtil.getColumnField(TokenTxCount_.UNIT)),
                  field(entityUtil.getColumnField(TokenTxCount_.TX_COUNT)))
              .values(tokenTxCount.getUnit(), tokenTxCount.getTxCount())
              .onConflict(field(entityUtil.getColumnField(TokenTxCount_.UNIT)))
              .doUpdate()
              .set(
                  field(entityUtil.getColumnField(TokenTxCount_.TX_COUNT)),
                  tokenTxCount.getTxCount());

      queries.add(query);
    }

    dsl.batch(queries).execute();
  }
}
