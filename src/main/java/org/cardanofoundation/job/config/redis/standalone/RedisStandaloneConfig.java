package org.cardanofoundation.job.config.redis.standalone;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Log4j2
@Configuration
@Profile("standalone")
public class RedisStandaloneConfig {

  @Value("${spring.redis.standalone.host}")
  private String hostname;

  @Value("${spring.redis.standalone.port}")
  private Integer port;

  @Value("${spring.redis.password}")
  private String password;

  @Value("${spring.redis.standalone.useSsl}")
  private boolean useSsl;

  @Bean
  RedisStandaloneConfiguration redisStandaloneConfiguration() {
    RedisStandaloneConfiguration redisStandaloneConfiguration =
        new RedisStandaloneConfiguration(hostname, port);
    redisStandaloneConfiguration.setPassword(password);
    return redisStandaloneConfiguration;
  }

  @Bean(name = "lettuceConnectionFactory")
  LettuceConnectionFactory lettuceConnectionFactory(
      RedisStandaloneConfiguration redisStandaloneConfiguration) {
    if (useSsl) {
      return new LettuceConnectionFactory(
          redisStandaloneConfiguration, LettuceClientConfiguration.builder().useSsl().build());
    } else {
      return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }
  }

  @Bean
  @Autowired
  RedisTemplate<String, ?> redisTemplate( // NOSONAR
      final LettuceConnectionFactory lettuceConnectionFactory) {
    var redisTemplate = new RedisTemplate<String, Object>();
    redisTemplate.setConnectionFactory(lettuceConnectionFactory);
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    redisTemplate.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
    redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    return redisTemplate;
  }
}
