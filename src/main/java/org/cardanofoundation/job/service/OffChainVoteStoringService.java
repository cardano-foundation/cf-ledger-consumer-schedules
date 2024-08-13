package org.cardanofoundation.job.service;

import java.util.Collection;

public abstract class OffChainVoteStoringService<S, F> {

  public abstract void insertFetchSuccessData(Collection<S> offChainAnchorData);

  public abstract void insertFetchFailData(Collection<F> offChainFetchErrorData);
}
