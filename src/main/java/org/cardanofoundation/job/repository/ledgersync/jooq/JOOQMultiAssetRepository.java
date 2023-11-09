package org.cardanofoundation.job.repository.ledgersync.jooq;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.BaseEntity_;
import org.cardanofoundation.explorer.consumercommon.entity.MultiAsset;
import org.cardanofoundation.job.util.EntityUtil;
import org.jooq.DSLContext;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

@Repository
public class JOOQMultiAssetRepository {

  private final DSLContext dsl;
  private final EntityUtil entityUtil;

  public JOOQMultiAssetRepository(@Qualifier("ledgerSyncDSLContext") DSLContext dsl,
                                  @Value("${spring.jpa.properties.hibernate.default_schema}") String schema) {
    this.dsl = dsl;
    this.entityUtil = new EntityUtil(schema, MultiAsset.class);
  }

  public List<MultiAsset> getMultiAsset(int page, int size) {
    var tableName = this.entityUtil.getTableName();
    return dsl.selectFrom(table(tableName))
        .orderBy(field(BaseEntity_.ID))
        .limit(size)
        .offset(page * size)
        .fetch()
        .into(MultiAsset.class);
  }
}
