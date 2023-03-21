package com.sotatek.cardano.job.cli;

import com.bloxbean.cardano.client.util.JsonUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Tip {

  int block;
  Integer epoch;
  String era;
  String hash;
  int slot;
  String syncProgress;

  @Override
  public String toString() {
    return JsonUtil.getPrettyJson(this);
  }
}
