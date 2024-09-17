package org.cardanofoundation.job.repository.explorer.jooq;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import org.jooq.DSLContext;

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

  public void upsertCheckpointByType(DataCheckpoint dataCheckpoint) {
    if (Objects.isNull(dataCheckpoint)) {
      return;
    }

    var query =
        dsl.insertInto(table(entityUtil.getTableName()))
            .columns(
                field(entityUtil.getColumnField(DataCheckpoint_.TYPE)),
                field(entityUtil.getColumnField(DataCheckpoint_.SLOT_NO)),
                field(entityUtil.getColumnField(DataCheckpoint_.UPDATE_TIME)))
            .values(
                dataCheckpoint.getType().getValue(),
                dataCheckpoint.getSlotNo(),
                dataCheckpoint.getUpdateTime())
            .onConflict(field(entityUtil.getColumnField(DataCheckpoint_.TYPE)))
            .doUpdate()
            .set(
                field(entityUtil.getColumnField(DataCheckpoint_.SLOT_NO)),
                dataCheckpoint.getSlotNo())
            .set(
                field(entityUtil.getColumnField(DataCheckpoint_.UPDATE_TIME)),
                dataCheckpoint.getUpdateTime());
    dsl.batch(query).execute();
  }
}
