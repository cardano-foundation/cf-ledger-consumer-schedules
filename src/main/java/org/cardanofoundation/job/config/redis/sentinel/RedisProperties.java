package org.cardanofoundation.job.config.redis.sentinel;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author huynv
 * @since 04/08/2021
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "redis")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RedisProperties {

  Integer databaseIndex;

  String master;

  String password;

  List<SentinelNode> sentinels;

  Boolean testOnBorrow;

  Integer maxTotal;

  Integer maxIdle;

  Integer minIdle;

  Boolean testOnReturn;

  Boolean testWhileIdle;

  @Data
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class SentinelNode {

    String host;

    Integer port;

  }
}
