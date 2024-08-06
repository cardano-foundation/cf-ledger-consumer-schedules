package org.cardanofoundation.job.service.impl;

import jakarta.annotation.PostConstruct;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.cardanofoundation.explorer.common.entity.compositeKey.OffChainVoteFetchErrorId;
import org.cardanofoundation.explorer.common.entity.compositeKey.OffChainVoteGovActionDataId;
import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteFetchError;
import org.cardanofoundation.explorer.common.entity.ledgersync.OffChainVoteGovActionData;
import org.cardanofoundation.job.dto.govActionMetaData.GovActionMetaData;
import org.cardanofoundation.job.dto.govActionMetaData.OffChainFetchResult;
import org.cardanofoundation.job.service.OffChainVoteFetchingService;

@Service
@Primary
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OffChainVoteGovActionFetchingDataServiceImpl
    extends OffChainVoteFetchingService<OffChainVoteGovActionData, OffChainVoteFetchError> {

  private final ObjectMapper mapper = new ObjectMapper();

  @PostConstruct
  void setup() {
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public OffChainVoteGovActionData extractRawOffChainData(OffChainFetchResult offChainFetchResult) {
    OffChainVoteGovActionData offChainGovActionData = new OffChainVoteGovActionData();
    OffChainVoteGovActionDataId offChainVoteGovActionDataId =
        OffChainVoteGovActionDataId.builder()
            .anchorHash(offChainFetchResult.getAnchorHash())
            .anchorUrl(offChainFetchResult.getAnchorUrl())
            .build();

    offChainGovActionData.setId(offChainVoteGovActionDataId);
    offChainGovActionData.setAnchorUrl(offChainFetchResult.getAnchorUrl());
    offChainGovActionData.setAnchorHash(offChainFetchResult.getAnchorHash());
    offChainGovActionData.setRawData(offChainFetchResult.getRawData());
    try {
      GovActionMetaData govActionMetaData =
          mapper.readValue(offChainGovActionData.getRawData(), GovActionMetaData.class);
      if (govActionMetaData.getBody() != null) {
        offChainGovActionData.setTitle(govActionMetaData.getBody().getTitle());
        offChainGovActionData.setAbstractData(govActionMetaData.getBody().getAbstractContent());
        offChainGovActionData.setMotivation(govActionMetaData.getBody().getMotivation());
        offChainGovActionData.setRationale(govActionMetaData.getBody().getRationale());
      }
    } catch (JsonProcessingException e) {
      log.info("Error when parse data to object from URL: {}", e.getMessage());
    }
    return offChainGovActionData;
  }

  @Override
  public OffChainVoteFetchError extractFetchError(OffChainFetchResult offChainAnchorData) {
    OffChainVoteFetchError offChainVoteFetchError = new OffChainVoteFetchError();
    OffChainVoteFetchErrorId offChainVoteFetchErrorId =
        OffChainVoteFetchErrorId.builder()
            .anchorHash(offChainAnchorData.getAnchorHash())
            .anchorUrl(offChainAnchorData.getAnchorUrl())
            .build();

    offChainVoteFetchError.setId(offChainVoteFetchErrorId);
    offChainVoteFetchError.setAnchorUrl(offChainAnchorData.getAnchorUrl());
    offChainVoteFetchError.setAnchorHash(offChainAnchorData.getAnchorHash());
    offChainVoteFetchError.setFetchError(offChainAnchorData.getFetchFailError());
    return offChainVoteFetchError;
  }
}
