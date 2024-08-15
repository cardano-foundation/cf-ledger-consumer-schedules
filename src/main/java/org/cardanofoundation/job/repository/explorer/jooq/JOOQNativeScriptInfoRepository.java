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

import org.cardanofoundation.explorer.common.entity.explorer.NativeScriptInfo;
import org.cardanofoundation.explorer.common.entity.explorer.NativeScriptInfo_;
import org.cardanofoundation.explorer.common.utils.EntityUtil;

@Repository
public class JOOQNativeScriptInfoRepository {

  private final DSLContext dsl;
  private final EntityUtil entityUtil;

  public JOOQNativeScriptInfoRepository(
      @Qualifier("explorerDSLContext") DSLContext dsl,
      @Value("${multi-datasource.datasourceExplorer.flyway.schemas}") String schema) {
    this.dsl = dsl;
    this.entityUtil = new EntityUtil(schema, NativeScriptInfo.class);
  }

  public void insertAll(List<NativeScriptInfo> nativeScriptInfos) {
    if (nativeScriptInfos.isEmpty()) {
      return;
    }

    List<Query> queries = new ArrayList<>();

    for (NativeScriptInfo nativeScriptInfo : nativeScriptInfos) {
      var query =
          dsl.insertInto(table(entityUtil.getTableName()))
              .columns(
                  field(entityUtil.getColumnField(NativeScriptInfo_.SCRIPT_HASH)),
                  field(entityUtil.getColumnField(NativeScriptInfo_.TYPE)),
                  field(entityUtil.getColumnField(NativeScriptInfo_.NUMBER_OF_ASSET_HOLDERS)),
                  field(entityUtil.getColumnField(NativeScriptInfo_.NUMBER_OF_TOKENS)),
                  field(entityUtil.getColumnField(NativeScriptInfo_.AFTER_SLOT)),
                  field(entityUtil.getColumnField(NativeScriptInfo_.BEFORE_SLOT)),
                  field(entityUtil.getColumnField(NativeScriptInfo_.NUMBER_SIG)))
              .values(
                  nativeScriptInfo.getScriptHash(),
                  nativeScriptInfo.getType().getValue(),
                  nativeScriptInfo.getNumberOfAssetHolders(),
                  nativeScriptInfo.getNumberOfTokens(),
                  nativeScriptInfo.getAfterSlot(),
                  nativeScriptInfo.getBeforeSlot(),
                  nativeScriptInfo.getNumberSig())
              .onConflict(field(entityUtil.getColumnField(NativeScriptInfo_.SCRIPT_HASH)))
              .doUpdate()
              .set(
                  field(entityUtil.getColumnField(NativeScriptInfo_.NUMBER_OF_ASSET_HOLDERS)),
                  nativeScriptInfo.getNumberOfAssetHolders())
              .set(
                  field(entityUtil.getColumnField(NativeScriptInfo_.NUMBER_OF_TOKENS)),
                  nativeScriptInfo.getNumberOfTokens());

      queries.add(query);
    }

    dsl.batch(queries).execute();
  }
}
