package org.cardanofoundation.job.dto.report.pool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.cardanofoundation.job.util.report.ColumnFieldEnum;
import org.cardanofoundation.job.util.report.ColumnTitleEnum;
import org.cardanofoundation.job.util.report.ExportColumn;
import org.cardanofoundation.job.util.report.ExportColumn.Alignment;

@Setter
@Getter
@Builder
public class PoolRegistration {

  private String txHash;

  private Date time;

  private BigDecimal adaValueHold;

  private BigDecimal adaValueFee;

  private BigDecimal adaValue;

  private Double rawAdaValue;

  private String owner;

  public static PoolRegistration toDomain(TabularRegisResponse response) {
    PoolRegistration result =
        PoolRegistration.builder()
            .txHash(response.getTxHash())
            .time(response.getTime())
            .adaValueHold(new BigDecimal(response.getDeposit()))
            .adaValueFee(new BigDecimal(response.getFee()))
            .build();
    result.setAdaValue(result.getAdaValueHold().add(result.getAdaValueFee()));
    result.setRawAdaValue(result.getAdaValue().doubleValue() / 1000000);
    return result;
  }

  public static List<ExportColumn> buildExportColumn() {
    List<ExportColumn> poolRegistrationsColumns = new ArrayList<>();
    poolRegistrationsColumns.add(
        new ExportColumn(
            ColumnFieldEnum.TX_HASH_COLUMN, ColumnTitleEnum.TX_HASH_TITLE, Alignment.LEFT));
    poolRegistrationsColumns.add(
        new ExportColumn(
            ColumnFieldEnum.TIME_COLUMN, ColumnTitleEnum.TIMESTAMP_TITLE, Alignment.CENTER));
    poolRegistrationsColumns.add(
        new ExportColumn(
            ColumnFieldEnum.ADA_VALUE_COLUMN, ColumnTitleEnum.ADA_VALUE_TITLE, Alignment.RIGHT));
    return poolRegistrationsColumns;
  }
}
