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
public class RewardDistribution {

  private String epoch;

  private Date time;

  private BigDecimal operatorReward;

  private Double rawOperatorReward;

  private String rewardAccount;

  public static RewardDistribution toDomain(RewardResponse response) {
    return RewardDistribution.builder()
        .epoch(response.getEpochNo().toString())
        .time(response.getTime())
        .operatorReward(new BigDecimal(response.getAmount()))
        .rewardAccount(response.getRewardAccount())
        .rawOperatorReward(new BigDecimal(response.getAmount()).doubleValue() / 1000000)
        .build();
  }

  public static List<ExportColumn> buildExportColumn() {
    List<ExportColumn> rewardDistributionColumns = new ArrayList<>();
    rewardDistributionColumns.add(
        new ExportColumn(
            ColumnFieldEnum.EPOCH_COLUMN, ColumnTitleEnum.EPOCH_TITLE, Alignment.RIGHT));
    rewardDistributionColumns.add(
        new ExportColumn(
            ColumnFieldEnum.TIME_COLUMN, ColumnTitleEnum.TIMESTAMP_TITLE, Alignment.CENTER));
    rewardDistributionColumns.add(
        new ExportColumn(
            ColumnFieldEnum.OPERATOR_REWARD_COLUMN,
            ColumnTitleEnum.OPERATOR_REWARD_TITLE,
            Alignment.RIGHT));
    rewardDistributionColumns.add(
        new ExportColumn(
            ColumnFieldEnum.REWARD_ACCOUNT_COLUMN,
            ColumnTitleEnum.REWARD_ACCOUNT_TITLE,
            Alignment.LEFT));
    return rewardDistributionColumns;
  }
}
