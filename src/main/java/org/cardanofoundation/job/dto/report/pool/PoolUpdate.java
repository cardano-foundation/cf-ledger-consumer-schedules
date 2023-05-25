package org.cardanofoundation.job.dto.report.pool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.cardanofoundation.job.util.report.ColumnFieldEnum;
import org.cardanofoundation.job.util.report.ColumnTitleEnum;
import org.cardanofoundation.job.util.report.ExportColumn;
import org.cardanofoundation.job.util.report.ExportColumn.Alignment;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PoolUpdate {

  private String txHash;

  private Date time;

  private BigDecimal adaValueHold;

  private BigDecimal adaValueFee;

  private BigDecimal adaValue;

  public static PoolUpdate toDomain(PoolUpdateDetailResponse response) {
    PoolUpdate result = PoolUpdate.builder()
        .txHash(response.getTxHash())
        .time(response.getTime())
        .adaValueHold(new BigDecimal(response.getPledge()))
        .adaValueFee(new BigDecimal(response.getFee()))
        .build();
    result.setAdaValue(result.getAdaValueHold().subtract(result.getAdaValueFee()));
    return result;
  }

  public static List<ExportColumn> buildExportColumn() {
    List<ExportColumn> poolUpdateColumns = new ArrayList<>();
    poolUpdateColumns.add(
        new ExportColumn(ColumnFieldEnum.TX_HASH_COLUMN, ColumnTitleEnum.TX_HASH_TITLE,
                         Alignment.LEFT));
    poolUpdateColumns.add(
        new ExportColumn(ColumnFieldEnum.TIME_COLUMN, ColumnTitleEnum.TIMESTAMP_TITLE,
                         Alignment.CENTER));
    poolUpdateColumns.add(
        new ExportColumn(ColumnFieldEnum.ADA_VALUE_FEE_COLUMN,
                         ColumnTitleEnum.ADA_VALUE_FEE_TITLE,
                         Alignment.RIGHT));
    return poolUpdateColumns;
  }
}