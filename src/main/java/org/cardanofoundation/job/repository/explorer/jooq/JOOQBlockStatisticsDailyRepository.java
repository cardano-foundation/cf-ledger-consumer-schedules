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

import org.cardanofoundation.explorer.common.entity.explorer.BlockStatisticsDaily;
import org.cardanofoundation.explorer.common.entity.explorer.BlockStatisticsDaily_;
import org.cardanofoundation.explorer.common.utils.EntityUtil;

@Repository
public class JOOQBlockStatisticsDailyRepository {
  private final DSLContext dsl;
  private final EntityUtil entityUtil;

  public JOOQBlockStatisticsDailyRepository(
      @Qualifier("explorerDSLContext") DSLContext dsl,
      @Value("${multi-datasource.datasourceExplorer.flyway.schemas}") String schema) {
    this.dsl = dsl;
    this.entityUtil = new EntityUtil(schema, BlockStatisticsDaily.class);
  }

  public void insertAll(List<BlockStatisticsDaily> blockStatisticsDailyList) {
    if (blockStatisticsDailyList.isEmpty()) {
      return;
    }

    List<Query> queries = new ArrayList<>();

    for (BlockStatisticsDaily blockStatisticsDaily : blockStatisticsDailyList) {
      var query =
          dsl.insertInto(table(entityUtil.getTableName()))
              .columns(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.EPOCH_NO)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.TIME)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.NO_REPORTING_NODES)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.NO_UNIQUE_PEERS)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.NO_COUNTRIES)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.NO_CONTINENTS)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_PROP_MEAN)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_PROP_MEDIAN)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_PROP_P90)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_PROP_P95)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_ADOPT_MEAN)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_ADOPT_MEDIAN)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_ADOPT_P90)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_ADOPT_P95)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.TXS)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.MEAN_SIZE_LOAD)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.MEAN_STEPS_LOAD)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.MEAN_MEM_LOAD)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.SLOT_BATTLES)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.HEIGHT_BATTLES)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.CDF3)),
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.CDF5)))
              .values(
                  blockStatisticsDaily.getEpochNo(),
                  blockStatisticsDaily.getTime(),
                  blockStatisticsDaily.getNoReportingNodes(),
                  blockStatisticsDaily.getNoUniquePeers(),
                  blockStatisticsDaily.getNoCountries(),
                  blockStatisticsDaily.getNoContinents(),
                  blockStatisticsDaily.getBlockPropMean(),
                  blockStatisticsDaily.getBlockPropMedian(),
                  blockStatisticsDaily.getBlockPropP90(),
                  blockStatisticsDaily.getBlockPropP95(),
                  blockStatisticsDaily.getBlockAdoptMean(),
                  blockStatisticsDaily.getBlockAdoptMedian(),
                  blockStatisticsDaily.getBlockAdoptP90(),
                  blockStatisticsDaily.getBlockAdoptP95(),
                  blockStatisticsDaily.getTxs(),
                  blockStatisticsDaily.getMeanSizeLoad(),
                  blockStatisticsDaily.getMeanStepsLoad(),
                  blockStatisticsDaily.getMeanMemLoad(),
                  blockStatisticsDaily.getSlotBattles(),
                  blockStatisticsDaily.getHeightBattles(),
                  blockStatisticsDaily.getCdf3(),
                  blockStatisticsDaily.getCdf5())
              .onConflict(field(entityUtil.getColumnField(BlockStatisticsDaily_.TIME)))
              .doUpdate()
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.EPOCH_NO)),
                  blockStatisticsDaily.getEpochNo())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.NO_REPORTING_NODES)),
                  blockStatisticsDaily.getNoReportingNodes())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.NO_UNIQUE_PEERS)),
                  blockStatisticsDaily.getNoUniquePeers())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.NO_COUNTRIES)),
                  blockStatisticsDaily.getNoCountries())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.NO_CONTINENTS)),
                  blockStatisticsDaily.getNoContinents())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_PROP_MEAN)),
                  blockStatisticsDaily.getBlockPropMean())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_PROP_MEDIAN)),
                  blockStatisticsDaily.getBlockPropMedian())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_PROP_P90)),
                  blockStatisticsDaily.getBlockPropP90())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_PROP_P95)),
                  blockStatisticsDaily.getBlockPropP95())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_ADOPT_MEAN)),
                  blockStatisticsDaily.getBlockAdoptMean())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_ADOPT_MEDIAN)),
                  blockStatisticsDaily.getBlockPropMedian())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_ADOPT_P90)),
                  blockStatisticsDaily.getBlockAdoptP90())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.BLOCK_ADOPT_P95)),
                  blockStatisticsDaily.getBlockAdoptP95())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.TXS)),
                  blockStatisticsDaily.getTxs())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.MEAN_SIZE_LOAD)),
                  blockStatisticsDaily.getMeanSizeLoad())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.MEAN_STEPS_LOAD)),
                  blockStatisticsDaily.getMeanStepsLoad())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.MEAN_MEM_LOAD)),
                  blockStatisticsDaily.getMeanMemLoad())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.SLOT_BATTLES)),
                  blockStatisticsDaily.getSlotBattles())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.HEIGHT_BATTLES)),
                  blockStatisticsDaily.getHeightBattles())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.CDF3)),
                  blockStatisticsDaily.getCdf3())
              .set(
                  field(entityUtil.getColumnField(BlockStatisticsDaily_.CDF5)),
                  blockStatisticsDaily.getCdf5());

      queries.add(query);
    }

    dsl.batch(queries).execute();
  }
}
