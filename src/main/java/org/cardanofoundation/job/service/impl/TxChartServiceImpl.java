package org.cardanofoundation.job.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Service;

import org.cardanofoundation.job.repository.ledgersync.TxChartRepository;
import org.cardanofoundation.job.service.TxChartService;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TxChartServiceImpl implements TxChartService {

  private final TxChartRepository txChartRepository;

  @Override
  public void refreshDataForTxChart() {
    txChartRepository.refreshMaterializedView();
  }
}
