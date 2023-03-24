package com.sotatek.cardano.job.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sotatek.cardano.job.constant.JobConstants;
import com.sotatek.cardano.job.service.interfaces.LedgerStateReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LedgerStateReaderImpl implements LedgerStateReader {

  private final ObjectMapper mapper;

  @Override
  public Map<String, Object> readFile(String fileName) throws InterruptedException, IOException {
    File file = new File(fileName);
    if (file.exists()) {
      var loopTime = 0;

      while (!file.canRead()) {
        if (loopTime == BigInteger.TEN.intValue()) {
          return Collections.emptyMap();
        }
        Thread.sleep(Duration.ofSeconds(BigInteger.TWO.longValue()).toMillis());
        loopTime = loopTime + 1;
      }

      StringBuilder builder = new StringBuilder();
      try (InputStreamReader inputStream = new InputStreamReader(new FileInputStream(file),
          StandardCharsets.UTF_8);) {
        char[] characters = new char[JobConstants.CHARACTERS];
        while (inputStream.ready()) {
          inputStream.read(characters);
          builder.append(characters);
        }
      } catch (Exception e) {
        log.error(e.getMessage());
      }

      return mapper.readValue(builder.toString(),
          new TypeReference<>() {
          });

    }
    return Collections.emptyMap();
  }
}
