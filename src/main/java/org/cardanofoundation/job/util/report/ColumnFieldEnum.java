package org.cardanofoundation.job.util.report;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
/**
 * This Enum class is used to map the column name in the CSV file to the field name of the data
 * object Each enum value should have the same name as the field name of the data object Each enum
 * value should use for one field name of the one specific data object
 */
public enum ColumnFieldEnum {
  TX_HASH_COLUMN("txHash"),
  EPOCH_COLUMN("epoch"),
  TIME_COLUMN("time"),
  AMOUNT_COLUMN("rawAmount"),
  WITHDRAWN_COLUMN("rawValue"),
  DEPOSIT_COLUMN("rawDeposit"),
  FEE_COLUMN("rawFee"),
  TYPE_COLUMN("type"),
  STATUS_COLUMN("status"),
  EPOCH_NO_COLUMN("epochNo"),
  OUT_SUM_COLUMN("rawOutSum"),
  SIZE_COLUMN("rawSize"),
  ADA_VALUE_COLUMN("rawAdaValue"),
  ADA_VALUE_HOLD_COLUMN("rawAdaValueHold"),
  ADA_VALUE_FEE_COLUMN("rawAdaValueFee"),
  OWNER_COLUMN("owner"),
  OPERATOR_REWARD_COLUMN("rawOperatorReward"),
  REWARD_ACCOUNT_COLUMN("rewardAccount"),
  POOL_NAME_COLUMN("poolName"),
  CREATED_AT_COLUMN("createdAt"),
  REPORT_TYPE_COLUMN("reportType"),
  STAKE_ADDRESS_COLUMN("stakeAddress"),
  POOL_ID_COLUMN("poolId"),
  REPORT_NAME_COLUMN("reportName"),
  DATE_RANGE_COLUMN("dateRange"),
  EPOCH_RANGE_COLUMN("epochRange"),
  DATE_TIME_FORTMAT_COLUMN("dateTimeFormat"),
  EVENTS_COLUMN("events"),
  ADA_TRANSFERS_COLUMN("isADATransfers"),
  POOL_SIZE_COLUMN("isPoolSize");
  private String value;
}
