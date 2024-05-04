package org.cardanofoundation.job.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Service;

import org.cardanofoundation.job.repository.ledgersync.TxCountRepository;
import org.cardanofoundation.job.service.TxCountService;
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TxCountServiceImpl implements TxCountService {

  private final TxCountRepository txCountRepository;

  @Override
  public void refreshDataForTxCount() {
      txCountRepository.refreshMaterializedView();
  }
}
