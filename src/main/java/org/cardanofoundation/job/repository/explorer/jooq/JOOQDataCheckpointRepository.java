package org.cardanofoundation.job.repository.explorer.jooq;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import org.jooq.DSLContext;
import org.jooq.Query;

import org.cardanofoundation.explorer.common.entity.explorer.DataCheckpoint;
import org.cardanofoundation.explorer.common.entity.explorer.DataCheckpoint_;
import org.cardanofoundation.explorer.common.utils.EntityUtil;

@Repository
public class JOOQDataCheckpointRepository {
  private final DSLContext dsl;
  private final EntityUtil entityUtil;

  public JOOQDataCheckpointRepository(
      @Qualifier("explorerDSLContext") DSLContext dsl,
      @Value("${multi-datasource.datasourceExplorer.flyway.schemas}") String schema) {
    this.dsl = dsl;
    this.entityUtil = new EntityUtil(schema, DataCheckpoint.class);
  }

  public void insertAll(List<DataCheckpoint> dataCheckpoints) {
    if (dataCheckpoints.isEmpty()) {
      return;
    }

    List<Query> queries = new ArrayList<>();

    for (DataCheckpoint dataCheckpoint : dataCheckpoints) {
      var query =
          dsl.insertInto(table(entityUtil.getTableName()))
              .columns(
                  field(entityUtil.getColumnField(DataCheckpoint_.UPDATE_TIME)),
                  field(entityUtil.getColumnField(DataCheckpoint_.TYPE)),
                  field(entityUtil.getColumnField(DataCheckpoint_.SLOT_NO)),
                  field(entityUtil.getColumnField(DataCheckpoint_.BLOCK_NO)))
              .values(
                  dataCheckpoint.getUpdateTime(),
                  dataCheckpoint.getType().getValue(),
                  dataCheckpoint.getSlotNo(),
                  dataCheckpoint.getBlockNo())
              .onConflict(field(entityUtil.getColumnField(DataCheckpoint_.TYPE)))
              .doUpdate()
              .set(
                  field(entityUtil.getColumnField(DataCheckpoint_.SLOT_NO)),
                  dataCheckpoint.getSlotNo())
              .set(
                  field(entityUtil.getColumnField(DataCheckpoint_.UPDATE_TIME)),
                  dataCheckpoint.getUpdateTime())
              .set(
                  field(entityUtil.getColumnField(DataCheckpoint_.BLOCK_NO)),
                  dataCheckpoint.getBlockNo());

      queries.add(query);
    }

    dsl.batch(queries).execute();
  }
}
