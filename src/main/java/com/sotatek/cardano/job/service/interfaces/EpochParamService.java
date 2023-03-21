package com.sotatek.cardano.job.service.interfaces;

import java.util.LinkedHashMap;

public interface EpochParamService {
  void handleEpochParam(LinkedHashMap<String,Object> map);
}
