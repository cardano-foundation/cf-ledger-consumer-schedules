package org.cardanofoundation.job.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.stereotype.Service;

import org.cardanofoundation.explorer.consumercommon.entity.StakeKeyReportHistory;
import org.cardanofoundation.job.service.interfaces.StakeKeyReportService;

@Service
@Log4j2
@RequiredArgsConstructor
public class StakeKeyReportServiceImpl implements StakeKeyReportService {


  @Override
  public void exportStakeKeyReport(StakeKeyReportHistory stakeKeyReportHistory) {
  }
}
