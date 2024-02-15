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

import org.cardanofoundation.explorer.common.entity.explorer.TokenInfo;
import org.cardanofoundation.explorer.common.entity.explorer.TokenInfo_;
import org.cardanofoundation.explorer.common.utils.EntityUtil;

@Repository
public class JOOQTokenInfoRepository {

  private final DSLContext dsl;
  private final EntityUtil entityUtil;

  public JOOQTokenInfoRepository(
      @Qualifier("explorerDSLContext") DSLContext dsl,
      @Value("${spring.jpa.properties.hibernate.default_schema}") String schema) {
    this.dsl = dsl;
    this.entityUtil = new EntityUtil(schema, TokenInfo.class);
  }

  public void insertAll(List<TokenInfo> tokenInfos) {
    if (tokenInfos.isEmpty()) {
      return;
    }

    List<Query> queries = new ArrayList<>();

    for (TokenInfo tokenInfo : tokenInfos) {
      var query =
          dsl.insertInto(table(entityUtil.getTableName()))
              .columns(
                  field(entityUtil.getColumnField(TokenInfo_.BLOCK_NO)),
                  field(entityUtil.getColumnField(TokenInfo_.MULTI_ASSET_ID)),
                  field(entityUtil.getColumnField(TokenInfo_.NUMBER_OF_HOLDERS)),
                  field(entityUtil.getColumnField(TokenInfo_.VOLUME24H)),
                  field(entityUtil.getColumnField(TokenInfo_.UPDATE_TIME)))
              .values(
                  tokenInfo.getBlockNo(),
                  tokenInfo.getMultiAssetId(),
                  tokenInfo.getNumberOfHolders(),
                  tokenInfo.getVolume24h(),
                  tokenInfo.getUpdateTime());

      queries.add(query);
    }

    dsl.batch(queries).execute();
  }
}
