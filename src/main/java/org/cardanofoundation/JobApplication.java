package org.cardanofoundation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@SpringBootApplication
public class JobApplication {

  public static void main(String[] args) {
    SpringApplication.run(JobApplication.class, args);
  }
}
