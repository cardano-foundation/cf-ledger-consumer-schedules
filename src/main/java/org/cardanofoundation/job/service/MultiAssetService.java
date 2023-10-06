package org.cardanofoundation.job.service;

import java.util.List;
import java.util.Map;

public interface MultiAssetService {

  Map<Long, Long> getMapNumberHolder(List<Long> multiAssetIds);
}
