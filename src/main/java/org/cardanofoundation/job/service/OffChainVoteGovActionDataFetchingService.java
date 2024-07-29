package org.cardanofoundation.job.service;

import java.util.Queue;

import org.cardanofoundation.job.dto.govActionMetaData.OffChainGovActionData;

public interface OffChainVoteGovActionDataFetchingService {

  Queue<OffChainGovActionData> getDataFromAnchorUrl();

  void retryFetchGovActionMetadata();
}
