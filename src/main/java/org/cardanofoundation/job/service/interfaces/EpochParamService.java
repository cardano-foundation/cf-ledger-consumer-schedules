package org.cardanofoundation.job.service.interfaces;

import java.util.LinkedHashMap;

public interface EpochParamService {
  void handleEpochParam(LinkedHashMap<String, Object> map);
}
