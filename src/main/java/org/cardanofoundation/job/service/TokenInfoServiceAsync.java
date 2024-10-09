package org.cardanofoundation.job.service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.explorer.common.entity.ledgersync.TokenTxCount;
import org.cardanofoundation.job.repository.ledgersyncagg.AddressTxAmountRepository;

@Component
@RequiredArgsConstructor
@Log4j2
public class TokenInfoServiceAsync {

  private final AddressTxAmountRepository addressTxAmountRepository;

  @Async
  @Transactional(readOnly = true)
  public CompletableFuture<List<TokenTxCount>> buildTokenTxCountList(List<String> units) {
    long startTime = System.currentTimeMillis();
    List<TokenTxCount> tokenTxCounts = addressTxAmountRepository.getTotalTxCountByUnitIn(units);
    log.info(
        "buildTokenTxCountList size: {}, took: {}ms",
        tokenTxCounts.size(),
        System.currentTimeMillis() - startTime);
    return CompletableFuture.completedFuture(tokenTxCounts);
  }
}
