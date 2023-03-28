package com.sotatek.cardano.job.service.impl;

import com.sotatek.cardano.job.service.interfaces.EpochParamService;
import com.sotatek.cardano.job.service.interfaces.KafkaService;
import java.util.LinkedHashMap;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

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
