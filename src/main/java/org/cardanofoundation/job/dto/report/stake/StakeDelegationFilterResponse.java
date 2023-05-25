package org.cardanofoundation.job.dto.report.stake;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.cardanofoundation.job.util.report.ColumnFieldEnum;
import org.cardanofoundation.job.util.report.ColumnTitleEnum;
import org.cardanofoundation.job.util.report.ExportColumn;
import org.cardanofoundation.job.util.report.ExportColumn.Alignment;

@Getter
@Setter
@Builder
public class StakeDelegationFilterResponse {
  private String txHash;
  private BigInteger outSum;
  private BigInteger fee;
  private LocalDateTime time;

  public static List<ExportColumn> buildExportColumn() {
    List<ExportColumn> columns = new ArrayList<>();
    columns.add(new ExportColumn(ColumnFieldEnum.TX_HASH_COLUMN, ColumnTitleEnum.TX_HASH_TITLE,
                                 Alignment.LEFT));
    columns.add(new ExportColumn(ColumnFieldEnum.TIME_COLUMN, ColumnTitleEnum.TIMESTAMP_TITLE,
                                 Alignment.CENTER));
    columns.add(new ExportColumn(ColumnFieldEnum.OUT_SUM_COLUMN, ColumnTitleEnum.FEES_TITLE,
                                 Alignment.RIGHT));
    return columns;
  }

}
