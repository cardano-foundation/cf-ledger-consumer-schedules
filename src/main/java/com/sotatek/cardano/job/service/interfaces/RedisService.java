package com.sotatek.cardano.job.service.interfaces;

public interface RedisService {
  void saveValue(String key, Object value);

  <T> T getValue(String key, Class<T> clazz);
}
