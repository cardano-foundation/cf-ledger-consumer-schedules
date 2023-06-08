package org.cardanofoundation.job.dto.report.stake;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.cardanofoundation.job.common.enumeration.TxStatus;
import org.cardanofoundation.job.util.report.ColumnFieldEnum;
import org.cardanofoundation.job.util.report.ColumnTitleEnum;
import org.cardanofoundation.job.util.report.ExportColumn;
import org.cardanofoundation.job.util.report.ExportColumn.Alignment;

@Getter
@Setter
public class StakeWalletActivityResponse implements Serializable {

  private String txHash;
  private BigInteger amount;
  private Double rawAmount;
  private BigInteger fee;
  private LocalDateTime time;
  private TxStatus status;

  public static List<ExportColumn> buildExportColumn() {
    List<ExportColumn> columns = new ArrayList<>();
    columns.add(
        new ExportColumn(
            ColumnFieldEnum.AMOUNT_COLUMN, ColumnTitleEnum.AMOUNT_ADA_TITLE, Alignment.RIGHT));
    columns.add(
        new ExportColumn(
            ColumnFieldEnum.TIME_COLUMN, ColumnTitleEnum.TIMESTAMP_TITLE, Alignment.CENTER));
    columns.add(
        new ExportColumn(
            ColumnFieldEnum.TX_HASH_COLUMN, ColumnTitleEnum.TX_HASH_TITLE, Alignment.LEFT));
    columns.add(
        new ExportColumn(
            ColumnFieldEnum.STATUS_COLUMN, ColumnTitleEnum.STATUS_TITLE, Alignment.CENTER));
    return columns;
  }
}
