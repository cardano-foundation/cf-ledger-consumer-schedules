package org.cardanofoundation.job.service.interfaces;

public interface EpochStakeService {

  void handleEpoch(Integer epochNo);

  Integer findMaxEpochNoStaked();
}
