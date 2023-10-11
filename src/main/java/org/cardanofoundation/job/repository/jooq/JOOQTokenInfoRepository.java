package org.cardanofoundation.job.repository.jooq;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.TokenInfo;
import org.cardanofoundation.explorer.consumercommon.entity.TokenInfo_;
import org.cardanofoundation.job.util.EntityUtil;
import org.jooq.DSLContext;
import org.jooq.Query;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

@Repository
public class JOOQTokenInfoRepository {

  private final DSLContext dsl;
  private final EntityUtil entityUtil;

  public JOOQTokenInfoRepository(DSLContext dsl,
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
      var query = dsl.insertInto(table(entityUtil.getTableName()))
          .columns(
              field(entityUtil.getColumnField(TokenInfo_.BLOCK_NO)),
              field(entityUtil.getColumnField(TokenInfo_.MULTI_ASSET_ID)),
              field(entityUtil.getColumnField(TokenInfo_.NUMBER_OF_HOLDERS)),
              field(entityUtil.getColumnField(TokenInfo_.VOLUME24H)),
              field(entityUtil.getColumnField(TokenInfo_.UPDATE_TIME))
          )
          .values(
              tokenInfo.getBlockNo(),
              tokenInfo.getMultiAssetId(),
              tokenInfo.getNumberOfHolders(),
              tokenInfo.getVolume24h(),
              tokenInfo.getUpdateTime()
          );

      queries.add(query);
    }

    dsl.batch(queries).execute();
  }
}
