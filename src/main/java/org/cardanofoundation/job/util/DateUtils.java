package org.cardanofoundation.job.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class DateUtils {

  public static Long timestampToEpochSecond(Timestamp timestamp) {
    return timestamp == null ? 0 : timestamp.toInstant().getEpochSecond();
  }

  public static LocalDateTime epochSecondToLocalDateTime(Long epochSecond) {
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC);
  }
}
