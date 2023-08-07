package org.cardanofoundation.job.config.properties;

import java.util.List;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "clouds")
@Configuration
public class S3Properties {

  List<S3Config> s3Configs;

  @Data
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class S3Config {
    String profile;
    String beanName;
    String accessKey;
    String secretKey;
    String region;
    String bucket;
    String endpoint;
  }
}
