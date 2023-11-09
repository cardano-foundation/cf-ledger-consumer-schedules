package org.cardanofoundation.job.repository.ledgersync.jooq;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.explorer.consumercommon.entity.AddressToken;
import org.cardanofoundation.explorer.consumercommon.entity.AddressTokenBalance_;
import org.cardanofoundation.explorer.consumercommon.entity.AddressToken_;
import org.cardanofoundation.job.model.TokenVolume;
import org.cardanofoundation.job.util.EntityUtil;
import org.jooq.DSLContext;

import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.sum;
import static org.jooq.impl.DSL.table;

@Repository
public class JOOQAddressTokenRepository {

  private final DSLContext dsl;
  private final EntityUtil entityUtil;

  public JOOQAddressTokenRepository(@Qualifier("ledgerSyncDSLContext") DSLContext dsl,
                                    @Value("${spring.jpa.properties.hibernate.default_schema}") String schema) {
    this.dsl = dsl;
    this.entityUtil = new EntityUtil(schema, AddressToken.class);
  }

  public List<TokenVolume> sumBalanceAfterTx(Collection<Long> multiAssetIds, Long txId) {
    var tableName = this.entityUtil.getTableName();
    var result = dsl.select(
            field(this.entityUtil.getColumnField(AddressTokenBalance_.MULTI_ASSET_ID)),
            coalesce(sum(field(this.entityUtil.getColumnField(AddressToken_.BALANCE)).cast(BigInteger.class)),
                0L).as(
                "volume"))
        .from(table(tableName))
        .where(field(this.entityUtil.getColumnField(AddressToken_.MULTI_ASSET_ID)).in(
            multiAssetIds))
        .and(field(this.entityUtil.getColumnField(AddressToken_.BALANCE)).gt(0))
        .and(field(this.entityUtil.getColumnField(AddressToken_.TX_ID)).greaterOrEqual(txId))
        .groupBy(field(this.entityUtil.getColumnField(AddressToken_.MULTI_ASSET_ID)))
        .fetch();

    return result.into(TokenVolume.class);
  }
}