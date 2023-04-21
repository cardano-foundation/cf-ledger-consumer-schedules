package com.sotatek.cardano.job.service.interfaces;

public interface EpochStakeService {

  void handleEpoch(Integer epochNo);

  Integer findMaxEpochNoStaked();
}
