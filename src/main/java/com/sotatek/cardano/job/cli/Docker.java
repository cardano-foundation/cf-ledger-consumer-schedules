package com.sotatek.cardano.job.cli;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "docker")
public class Docker {

  String host;
  String imageId;
  String cardanoNodeName;
  String[] portSpecs;
  String exposedPorts;
  String[] environments;
}
