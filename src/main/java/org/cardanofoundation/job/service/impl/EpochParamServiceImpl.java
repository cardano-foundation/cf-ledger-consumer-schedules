package org.cardanofoundation.job.service.impl;

import java.util.LinkedHashMap;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Service;

import org.cardanofoundation.job.service.interfaces.EpochParamService;
import org.cardanofoundation.job.service.interfaces.KafkaService;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class EpochParamServiceImpl implements EpochParamService {

  private final KafkaService kafkaService;

  @Override
  public void handleEpochParam(LinkedHashMap<String, Object> map) {
    // Do extract epoch param in here
  }
}
