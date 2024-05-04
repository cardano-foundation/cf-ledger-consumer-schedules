package org.cardanofoundation.job.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Service;

import org.cardanofoundation.job.repository.ledgersync.AddressTxCountRepository;
import org.cardanofoundation.job.service.AddressService;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressServiceImpl implements AddressService {

  private final AddressTxCountRepository addressTxCountRepository;

  @Override
  public void refreshDataForAddressTxCount() {
    addressTxCountRepository.refreshMaterializedView();
  }
}
