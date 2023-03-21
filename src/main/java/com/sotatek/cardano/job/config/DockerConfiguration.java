package com.sotatek.cardano.job.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.sotatek.cardano.job.cli.Docker;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class DockerConfiguration {

  Docker docker;

  @Bean
  DockerClient dockerClient(){
    final DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerHost(docker.getHost())
        .build();

    return DockerClientBuilder
        .getInstance(config)
        .build();
  }
}
