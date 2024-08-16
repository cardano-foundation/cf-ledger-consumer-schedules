package org.cardanofoundation.job.service;

import java.util.Map;
import java.util.Set;

public interface MultiAssetService {

  Map<String, Long> getMapNumberHolderByUnits(Set<String> units);
}
