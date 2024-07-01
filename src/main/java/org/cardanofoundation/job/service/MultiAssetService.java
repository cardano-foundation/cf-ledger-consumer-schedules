package org.cardanofoundation.job.service;

import java.util.List;
import java.util.Map;

public interface MultiAssetService {

  Map<String, Long> getMapNumberHolderByUnits(List<String> units);
}
