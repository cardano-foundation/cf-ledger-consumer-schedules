package org.cardanofoundation.job.service;

import java.util.List;
import java.util.Queue;

import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteFetchError;
import org.cardanofoundation.job.dto.govActionMetaData.OffChainGovActionData;

public interface OffChainVoteGovActionDataStoringService {
  void insertData(Queue<OffChainGovActionData> offChainVoteGovActionData);

  void insertFetchRetryData(
      List<OffChainVoteFetchError> offChainVoteFetchError,
      Queue<OffChainGovActionData> offChainGovActionDataListRetry);
}
