package org.cardanofoundation.job.dto.report.pool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.cardanofoundation.job.util.DataUtil;
import org.cardanofoundation.job.util.report.ColumnFieldEnum;
import org.cardanofoundation.job.util.report.ColumnTitleEnum;
import org.cardanofoundation.job.util.report.ExportColumn;
import org.cardanofoundation.job.util.report.ExportColumn.Alignment;

@Getter
@Setter
@Builder
public class PoolDeregistration {

  private String txHash;

  private Date time;

  private BigDecimal adaValueHold;

  private BigDecimal adaValueFee;

  private BigDecimal adaValue;

  private Double rawAdaValueHold;

  private Double rawAdaValueFee;

  private String owner;

  public static PoolDeregistration toDomain(DeRegistrationResponse response) {
    PoolDeregistration result =
        PoolDeregistration.builder()
            .txHash(response.getTxHash())
            .time(response.getTime())
            .adaValueHold(
                DataUtil.isNullOrEmpty(response.getPoolHold())
                    ? new BigDecimal(0)
                    : new BigDecimal(response.getPoolHold()))
            .adaValueFee(new BigDecimal(response.getFee()))
            .owner(String.join("\n", response.getStakeKeys()))
            .build();

    result.setAdaValue(new BigDecimal(response.getTotalFee()));
    result.setRawAdaValueHold(result.getAdaValueHold().doubleValue() / 1000000);
    result.setRawAdaValueFee(result.getAdaValueFee().doubleValue() / 1000000);
    return result;
  }

  public static List<ExportColumn> buildExportColumn() {
    List<ExportColumn> deregistrationColumns = new ArrayList<>();
    deregistrationColumns.add(
        new ExportColumn(
            ColumnFieldEnum.TX_HASH_COLUMN, ColumnTitleEnum.TX_HASH_TITLE, Alignment.LEFT));
    deregistrationColumns.add(
        new ExportColumn(
            ColumnFieldEnum.TIME_COLUMN, ColumnTitleEnum.TIMESTAMP_TITLE, Alignment.CENTER));
    deregistrationColumns.add(
        new ExportColumn(
            ColumnFieldEnum.ADA_VALUE_HOLD_COLUMN,
            ColumnTitleEnum.ADA_VALUE_HOLD_TITLE,
            Alignment.RIGHT));
    deregistrationColumns.add(
        new ExportColumn(
            ColumnFieldEnum.ADA_VALUE_FEE_COLUMN,
            ColumnTitleEnum.ADA_VALUE_FEE_TITLE,
            Alignment.RIGHT));
    return deregistrationColumns;
  }
}
