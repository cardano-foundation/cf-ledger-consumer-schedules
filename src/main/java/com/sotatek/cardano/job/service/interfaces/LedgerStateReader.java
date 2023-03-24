package com.sotatek.cardano.job.service.interfaces;

import java.io.IOException;
import java.util.Map;

public interface LedgerStateReader {

  Map<String, Object> readFile(String fileName) throws InterruptedException, IOException;
}
