package org.cardanofoundation.job.dto.report.pool;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Builder;
import lombok.Data;

import org.cardanofoundation.job.util.report.ColumnFieldEnum;
import org.cardanofoundation.job.util.report.ColumnTitleEnum;
import org.cardanofoundation.job.util.report.ExportColumn;
import org.cardanofoundation.job.util.report.ExportColumn.Alignment;

@Builder
@Data
public class InformationReport {
  private Date createdAt;

  private String reportType;

  private String poolId;

  private String stakeAddress;

  private String reportName;

  private String dateRange;

  private String epochRange;

  private String dateTimeFormat;

  private String events;

  private String isADATransfers;

  private String isPoolSize;

  public static List<ExportColumn> buildExportColumn(Boolean isPoolReport) {
    List<ExportColumn> columns = new ArrayList<>();
    columns.add(
        new ExportColumn(
            ColumnFieldEnum.CREATED_AT_COLUMN, ColumnTitleEnum.CREATED_AT_TITLE, Alignment.LEFT));
    columns.add(
        new ExportColumn(
            ColumnFieldEnum.REPORT_TYPE_COLUMN, ColumnTitleEnum.REPORT_TYPE_TITLE, Alignment.LEFT));
    if (isPoolReport) {
      columns.add(
          new ExportColumn(
              ColumnFieldEnum.POOL_ID_COLUMN, ColumnTitleEnum.POOL_ID_TITLE, Alignment.LEFT));
    } else {
      columns.add(
          new ExportColumn(
              ColumnFieldEnum.STAKE_ADDRESS_COLUMN,
              ColumnTitleEnum.STAKE_ADDRESS_TITLE,
              Alignment.LEFT));
    }
    columns.add(
        new ExportColumn(
            ColumnFieldEnum.REPORT_NAME_COLUMN, ColumnTitleEnum.REPORT_NAME_TITLE, Alignment.LEFT));
    if (isPoolReport) {
      columns.add(
          new ExportColumn(
              ColumnFieldEnum.EPOCH_RANGE_COLUMN,
              ColumnTitleEnum.EPOCH_RANGE_TITLE,
              Alignment.LEFT));
    } else {
      columns.add(
          new ExportColumn(
              ColumnFieldEnum.DATE_RANGE_COLUMN, ColumnTitleEnum.DATE_RANGE_TITLE, Alignment.LEFT));
    }
    columns.add(
        new ExportColumn(
            ColumnFieldEnum.DATE_TIME_FORTMAT_COLUMN,
            ColumnTitleEnum.DATE_TIME_FORMAT_TITLE,
            Alignment.LEFT));
    columns.add(
        new ExportColumn(
            ColumnFieldEnum.EVENTS_COLUMN, ColumnTitleEnum.EVENTS_TITLE, Alignment.LEFT));
    if (isPoolReport) {
      columns.add(
          new ExportColumn(
              ColumnFieldEnum.POOL_SIZE_COLUMN, ColumnTitleEnum.POOL_SIZE_TITLE, Alignment.LEFT));
    } else {
      columns.add(
          new ExportColumn(
              ColumnFieldEnum.ADA_TRANSFERS_COLUMN,
              ColumnTitleEnum.ADA_TRANSFERS_TITLE,
              Alignment.LEFT));
    }
    return columns;
  }
}
