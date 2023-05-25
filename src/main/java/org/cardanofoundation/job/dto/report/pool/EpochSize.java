package org.cardanofoundation.job.dto.report.pool;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.cardanofoundation.job.projection.PoolReportProjection;
import org.cardanofoundation.job.util.report.ColumnFieldEnum;
import org.cardanofoundation.job.util.report.ColumnTitleEnum;
import org.cardanofoundation.job.util.report.ExportColumn;
import org.cardanofoundation.job.util.report.ExportColumn.Alignment;

@Getter
@Setter
@Builder
public class EpochSize {
  private String epoch;

  private BigInteger fee;

  private BigDecimal size;

  public static EpochSize toDomain(PoolReportProjection projection) {
    return EpochSize.builder()
        .epoch(projection.getEpochNo().toString())
        .fee(projection.getFee())
        .size(new BigDecimal(projection.getSize()))
        .build();
  }

  public static List<ExportColumn> buildExportColumn() {
    List<ExportColumn> epochSizeColumns = new ArrayList<>();
    epochSizeColumns.add(
        new ExportColumn(ColumnFieldEnum.EPOCH_COLUMN, ColumnTitleEnum.EPOCH_TITLE,
                         Alignment.RIGHT));
    epochSizeColumns.add(
        new ExportColumn(ColumnFieldEnum.SIZE_COLUMN, ColumnTitleEnum.SIZE_TITLE, Alignment.RIGHT));
    return epochSizeColumns;
  }
}
