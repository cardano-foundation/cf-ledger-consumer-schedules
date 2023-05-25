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

  private String owner;

  public static PoolRegistration toDomain(TabularRegisResponse response) {
    PoolRegistration result = PoolRegistration.builder()
        .txHash(response.getTxHash())
        .time(response.getTime())
        .adaValueHold(new BigDecimal(response.getDeposit()))
        .adaValueFee(new BigDecimal(response.getFee()))
        .owner(String.join("\n", response.getStakeKeys()))
        .build();
    result.setAdaValue(result.getAdaValueHold().subtract(result.getAdaValueFee()));
    return result;
  }

  public static List<ExportColumn> buildExportColumn() {
    List<ExportColumn> poolRegistrationsColumns = new ArrayList<>();
    poolRegistrationsColumns.add(
        new ExportColumn(ColumnFieldEnum.TX_HASH_COLUMN, ColumnTitleEnum.TX_HASH_TITLE,
                         Alignment.LEFT));
    poolRegistrationsColumns.add(
        new ExportColumn(ColumnFieldEnum.TIME_COLUMN, ColumnTitleEnum.TIMESTAMP_TITLE,
                         Alignment.CENTER));
    poolRegistrationsColumns.add(
        new ExportColumn(ColumnFieldEnum.ADA_VALUE_COLUMN, ColumnTitleEnum.ADA_VALUE_TITLE,
                         Alignment.RIGHT));
    poolRegistrationsColumns.add(
        new ExportColumn(ColumnFieldEnum.OWNER_COLUMN, ColumnTitleEnum.OWNER_TITLE,
                         Alignment.LEFT));
    return poolRegistrationsColumns;
  }
}