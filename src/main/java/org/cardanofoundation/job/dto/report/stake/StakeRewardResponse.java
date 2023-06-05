package org.cardanofoundation.job.dto.report.stake;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.cardanofoundation.job.util.report.ColumnFieldEnum;
import org.cardanofoundation.job.util.report.ColumnTitleEnum;
import org.cardanofoundation.job.util.report.ExportColumn;
import org.cardanofoundation.job.util.report.ExportColumn.Alignment;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class StakeRewardResponse {
  private Integer epoch;
  private Date time;
  private BigInteger amount;
  private Double rawAmount;

  public static List<ExportColumn> buildExportColumn() {
    List<ExportColumn> columns = new ArrayList<>();
    columns.add(
        new ExportColumn(
            ColumnFieldEnum.AMOUNT_COLUMN, ColumnTitleEnum.REWARDS_PAID_TITLE, Alignment.RIGHT));
    columns.add(
        new ExportColumn(
            ColumnFieldEnum.TIME_COLUMN, ColumnTitleEnum.TIMESTAMP_TITLE, Alignment.CENTER));
    columns.add(
        new ExportColumn(
            ColumnFieldEnum.EPOCH_COLUMN, ColumnTitleEnum.EPOCH_TITLE, Alignment.RIGHT));
    return columns;
  }
}
