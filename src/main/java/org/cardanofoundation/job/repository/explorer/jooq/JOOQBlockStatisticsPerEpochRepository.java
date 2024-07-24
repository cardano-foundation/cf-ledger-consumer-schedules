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

import org.cardanofoundation.explorer.common.entity.explorer.BlockStatisticsPerEpoch;
import org.cardanofoundation.explorer.common.entity.explorer.BlockStatisticsPerEpoch_;
import org.cardanofoundation.explorer.common.utils.EntityUtil;

@Repository
public class JOOQBlockStatisticsPerEpochRepository {
  private final DSLContext dsl;
  private final EntityUtil entityUtil;

  public JOOQBlockStatisticsPerEpochRepository(
      @Qualifier("explorerDSLContext") DSLContext dsl,
      @Value("${multi-datasource.datasourceExplorer.flyway.schemas}") String schema) {
    this.dsl = dsl;
    this.entityUtil = new EntityUtil(schema, BlockStatisticsPerEpoch.class);
  }

  public void insertAll(List<BlockStatisticsPerEpoch> blockStatisticsPerEpochList) {
    if (blockStatisticsPerEpochList.isEmpty()) {
      return;
    }

    List<Query> queries = new ArrayList<>();

    for (BlockStatisticsPerEpoch blockStatisticsPerEpoch : blockStatisticsPerEpochList) {
      var query =
          dsl.insertInto(table(entityUtil.getTableName()))
              .columns(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.EPOCH_NO)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.TIME)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.NO_REPORTING_NODES)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.NO_COUNTRIES)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.NO_CONTINENTS)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_PROP_MEAN)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_PROP_MEDIAN)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_PROP_P90)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_PROP_P95)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_ADOPT_MEAN)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_ADOPT_MEDIAN)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_ADOPT_P90)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_ADOPT_P95)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.TXS)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.MEAN_SIZE_LOAD)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.MEAN_STEPS_LOAD)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.MEAN_MEM_LOAD)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.SLOT_BATTLES)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.HEIGHT_BATTLES)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.CDF3)),
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.CDF5)))
              .values(
                  blockStatisticsPerEpoch.getEpochNo(),
                  blockStatisticsPerEpoch.getTime(),
                  blockStatisticsPerEpoch.getNoReportingNodes(),
                  blockStatisticsPerEpoch.getNoCountries(),
                  blockStatisticsPerEpoch.getNoContinents(),
                  blockStatisticsPerEpoch.getBlockPropMean(),
                  blockStatisticsPerEpoch.getBlockPropMedian(),
                  blockStatisticsPerEpoch.getBlockPropP90(),
                  blockStatisticsPerEpoch.getBlockPropP95(),
                  blockStatisticsPerEpoch.getBlockAdoptMean(),
                  blockStatisticsPerEpoch.getBlockAdoptMedian(),
                  blockStatisticsPerEpoch.getBlockAdoptP90(),
                  blockStatisticsPerEpoch.getBlockAdoptP95(),
                  blockStatisticsPerEpoch.getTxs(),
                  blockStatisticsPerEpoch.getMeanSizeLoad(),
                  blockStatisticsPerEpoch.getMeanStepsLoad(),
                  blockStatisticsPerEpoch.getMeanMemLoad(),
                  blockStatisticsPerEpoch.getSlotBattles(),
                  blockStatisticsPerEpoch.getHeightBattles(),
                  blockStatisticsPerEpoch.getCdf3(),
                  blockStatisticsPerEpoch.getCdf5())
              .onConflict(field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.EPOCH_NO)))
              .doUpdate()
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.TIME)),
                  blockStatisticsPerEpoch.getTime())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.NO_REPORTING_NODES)),
                  blockStatisticsPerEpoch.getNoReportingNodes())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.NO_COUNTRIES)),
                  blockStatisticsPerEpoch.getNoCountries())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.NO_CONTINENTS)),
                  blockStatisticsPerEpoch.getNoContinents())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_PROP_MEAN)),
                  blockStatisticsPerEpoch.getBlockPropMean())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_PROP_MEDIAN)),
                  blockStatisticsPerEpoch.getBlockPropMedian())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_PROP_P90)),
                  blockStatisticsPerEpoch.getBlockPropP90())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_PROP_P95)),
                  blockStatisticsPerEpoch.getBlockPropP95())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_ADOPT_MEAN)),
                  blockStatisticsPerEpoch.getBlockAdoptMean())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_ADOPT_MEDIAN)),
                  blockStatisticsPerEpoch.getBlockPropMedian())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_ADOPT_P90)),
                  blockStatisticsPerEpoch.getBlockAdoptP90())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.BLOCK_ADOPT_P95)),
                  blockStatisticsPerEpoch.getBlockAdoptP95())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.TXS)),
                  blockStatisticsPerEpoch.getTxs())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.MEAN_SIZE_LOAD)),
                  blockStatisticsPerEpoch.getMeanSizeLoad())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.MEAN_STEPS_LOAD)),
                  blockStatisticsPerEpoch.getMeanStepsLoad())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.MEAN_MEM_LOAD)),
                  blockStatisticsPerEpoch.getMeanMemLoad())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.SLOT_BATTLES)),
                  blockStatisticsPerEpoch.getSlotBattles())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.HEIGHT_BATTLES)),
                  blockStatisticsPerEpoch.getHeightBattles())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.CDF3)),
                  blockStatisticsPerEpoch.getCdf3())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsPerEpoch_.CDF5)),
                  blockStatisticsPerEpoch.getCdf5());

      queries.add(query);
    }

    dsl.batch(queries).execute();
  }
}
