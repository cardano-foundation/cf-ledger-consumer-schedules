package org.cardanofoundation.job.util;

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Hex;

import org.cardanofoundation.explorer.common.utils.StringUtil;

public class HexUtils {

  private HexUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Convert from hex to UTF-8
   *
   * @param hexString string hex
   * @return string UTF-8
   */
  public static String fromHex(String hexString, String fingerprint) {
    try {
      byte[] bytes = Hex.decodeHex(hexString.toCharArray());
      if (StringUtil.isUtf8(bytes)) {
        return new String(bytes, StandardCharsets.UTF_8);
      }
      return fingerprint;
    } catch (Exception ex) {
      return fingerprint;
    }
  }
}
