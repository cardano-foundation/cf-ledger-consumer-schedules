//package org.cardanofoundation.job.repository.ledgersync.jooq;
//
//import static org.jooq.impl.DSL.field;
//import static org.jooq.impl.DSL.table;
//
//import java.util.List;
//
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Repository;
//
//import org.jooq.DSLContext;
//import org.jooq.impl.DSL;
//
//import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTokenBalance;
//import org.cardanofoundation.explorer.common.entity.ledgersync.AddressTokenBalance_;
//import org.cardanofoundation.explorer.common.utils.EntityUtil;
//import org.cardanofoundation.job.model.TokenNumberHolders;
//
//@Repository
//public class JOOQAddressTokenBalanceRepository {
//
//  private final DSLContext dsl;
//  private final EntityUtil entityUtil;
//
//  public JOOQAddressTokenBalanceRepository(
//      @Qualifier("ledgerSyncDSLContext") DSLContext dsl,
//      @Value("${spring.jpa.properties.hibernate.default_schema}") String schema) {
//    this.dsl = dsl;
//    this.entityUtil = new EntityUtil(schema, AddressTokenBalance.class);
//  }
//
//  public List<TokenNumberHolders> countAddressNotHaveStakeByMultiAssetIn(List<Long> multiAssetIds) {
//    var tableName = this.entityUtil.getTableName();
//
//    var result =
//        dsl.select(
//                DSL.count(field(this.entityUtil.getColumnField(AddressTokenBalance_.ADDRESS_ID)))
//                    .as("numberOfHolders"),
//                field(this.entityUtil.getColumnField(AddressTokenBalance_.MULTI_ASSET_ID))
//                    .as("ident"))
//            .from(table(tableName))
//            .where(
//                field(this.entityUtil.getColumnField(AddressTokenBalance_.MULTI_ASSET_ID))
//                    .in(multiAssetIds))
//            .and(field(this.entityUtil.getColumnField(AddressTokenBalance_.STAKE_ADDRESS)).isNull())
//            .and(field(this.entityUtil.getColumnField(AddressTokenBalance_.BALANCE)).gt(0))
//            .groupBy(field(this.entityUtil.getColumnField(AddressTokenBalance_.MULTI_ASSET_ID)))
//            .fetch();
//
//    return result.into(TokenNumberHolders.class);
//  }
//
//  public List<TokenNumberHolders> countByMultiAssetIn(List<Long> multiAssetIds) {
//    var tableName = this.entityUtil.getTableName();
//    var result =
//        dsl.select(
//                DSL.countDistinct(
//                        field(this.entityUtil.getColumnField(AddressTokenBalance_.STAKE_ADDRESS)))
//                    .as("numberOfHolders"),
//                field(this.entityUtil.getColumnField(AddressTokenBalance_.MULTI_ASSET_ID))
//                    .as("ident"))
//            .from(table(tableName))
//            .where(
//                field(this.entityUtil.getColumnField(AddressTokenBalance_.MULTI_ASSET_ID))
//                    .in(multiAssetIds))
//            .and(field(this.entityUtil.getColumnField(AddressTokenBalance_.BALANCE)).gt(0))
//            .groupBy(field(this.entityUtil.getColumnField(AddressTokenBalance_.MULTI_ASSET_ID)))
//            .fetch();
//
//    return result.into(TokenNumberHolders.class);
//  }
//
//  public List<TokenNumberHolders> countAddressNotHaveStakeByMultiAssetBetween(
//      Long startIdent, Long endIdent) {
//    var tableName = this.entityUtil.getTableName();
//
//    var result =
//        dsl.select(
//                DSL.count(field(this.entityUtil.getColumnField(AddressTokenBalance_.ADDRESS_ID)))
//                    .as("numberOfHolders"),
//                field(this.entityUtil.getColumnField(AddressTokenBalance_.MULTI_ASSET_ID))
//                    .as("ident"))
//            .from(table(tableName))
//            .where(
//                field(this.entityUtil.getColumnField(AddressTokenBalance_.MULTI_ASSET_ID))
//                    .between(startIdent, endIdent))
//            .and(field(this.entityUtil.getColumnField(AddressTokenBalance_.STAKE_ADDRESS)).isNull())
//            .and(field(this.entityUtil.getColumnField(AddressTokenBalance_.BALANCE)).gt(0))
//            .groupBy(field(this.entityUtil.getColumnField(AddressTokenBalance_.MULTI_ASSET_ID)))
//            .fetch();
//
//    return result.into(TokenNumberHolders.class);
//  }
//
//  public List<TokenNumberHolders> countByMultiAssetBetween(Long startIdent, Long endIdent) {
//    var tableName = this.entityUtil.getTableName();
//    var result =
//        dsl.select(
//                DSL.countDistinct(
//                        field(this.entityUtil.getColumnField(AddressTokenBalance_.STAKE_ADDRESS)))
//                    .as("numberOfHolders"),
//                field(this.entityUtil.getColumnField(AddressTokenBalance_.MULTI_ASSET_ID))
//                    .as("ident"))
//            .from(table(tableName))
//            .where(
//                field(this.entityUtil.getColumnField(AddressTokenBalance_.MULTI_ASSET_ID))
//                    .between(startIdent, endIdent))
//            .and(field(this.entityUtil.getColumnField(AddressTokenBalance_.BALANCE)).gt(0))
//            .groupBy(field(this.entityUtil.getColumnField(AddressTokenBalance_.MULTI_ASSET_ID)))
//            .fetch();
//
//    return result.into(TokenNumberHolders.class);
//  }
//}
