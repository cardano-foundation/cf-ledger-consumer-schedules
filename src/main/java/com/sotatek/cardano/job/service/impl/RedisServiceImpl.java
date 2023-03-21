package com.sotatek.cardano.job.service.impl;

import com.sotatek.cardano.job.service.interfaces.RedisService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisServiceImpl implements RedisService {
  
  RedisTemplate<String, Object> redisTemplate;
  
  @Override
  public void saveValue(String key, Object value) {
    redisTemplate.opsForValue().set(key, value);
  }

  @Override
  public <T> T getValue(String key, Class<T> clazz) {
    return clazz.cast(redisTemplate.opsForValue().get(key));
  }
}
