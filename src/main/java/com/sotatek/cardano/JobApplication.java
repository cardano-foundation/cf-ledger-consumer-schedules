package com.sotatek.cardano;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@EnableConfigurationProperties
@ComponentScan(basePackages = "com.sotatek")
@SpringBootApplication
public class JobApplication {

  public static void main(String[] args) {
    SpringApplication.run(JobApplication.class, args);
  }
}
