package org.cardanofoundation.job.service;

import java.util.List;

import org.cardanofoundation.job.projection.PoolCountProjection;

public interface DelegationService {

  Integer countCurrentDelegator();

  List<PoolCountProjection> getAllLivePoolDelegatorsCount();
}
