package com.sotatek.cardano.job.service.interfaces;

import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

public interface EpochStakeService {

  /**
   * Extract data from ledger state json dump file. <p> Find state before get field es snapshots , p
   * stake mark. <p> Then extract epoch stake by get stake field. <p> After that find stake pool in
   * delegations with same key (key hash or script hash) <p>
   *
   * @param map ledger state in json
   */
  @Transactional
  void handleLedgerState(Map<String, Object> map);

  Integer findMaxEpochNoStaked();
}
