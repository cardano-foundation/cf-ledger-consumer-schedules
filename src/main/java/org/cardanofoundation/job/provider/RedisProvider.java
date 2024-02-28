package org.cardanofoundation.job.provider;

import java.util.Map;
import java.util.Set;

import lombok.NonNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisProvider<K, V> {
  @Value("${application.network}")
  String network;

  @Autowired private RedisTemplate<K, V> redisTemplate;

  public V getValueByKey(@NonNull K key) {
    return redisTemplate.opsForValue().get(key);
  }

  public void setValueByKey(@NonNull K key, @NonNull V value) {
    redisTemplate.opsForValue().set(key, value);
  }

  public <HK, HV> void putAllHashByKey(
      @NonNull K key, @NonNull Map<? extends HK, ? extends HV> values) {
    redisTemplate.opsForHash().putAll(key, values);
  }

  public Set<K> keys(@NonNull K keys) {
    return redisTemplate.keys(keys);
  }

  public void del(@NonNull Set<K> keys) {
    redisTemplate.delete(keys);
  }

  public Boolean hasKey(@NonNull K key) {
    return redisTemplate.hasKey(key);
  }

  public boolean del(@NonNull K key) {
    return Boolean.TRUE.equals(redisTemplate.delete(key));
  }

  public String getRedisKey(String prefix) {
    return prefix + "_" + network;
  }
}
