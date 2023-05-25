package org.cardanofoundation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@SpringBootApplication
public class CardanoScheduleApplication {

  public static void main(String[] args) {
    SpringApplication.run(CardanoScheduleApplication.class, args);
  }
}
