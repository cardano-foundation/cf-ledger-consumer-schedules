package org.cardanofoundation.job.config;

import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

@TestConfiguration
public class RedisTestConfig {

  private static final RedisServer redisServer;

  static {
    redisServer = RedisServer.builder().port(6381).build();
    redisServer.start();
  }
}
