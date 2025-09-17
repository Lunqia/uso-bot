package dev.lunqia.usobot.overwatch2;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class OverfastApiCache {
  private final Map<OverfastApiEndpointType, Cache<String, Object>> caches =
      new ConcurrentHashMap<>();

  public <T> void put(
      OverfastApiEndpointType overfastApiEndpointType, String key, T value, long ttlSeconds) {
    caches
        .computeIfAbsent(
            overfastApiEndpointType,
            k -> Caffeine.newBuilder().expireAfterWrite(ttlSeconds, TimeUnit.SECONDS).build())
        .put(key, value);
  }

  @SuppressWarnings("unchecked")
  public <T> T get(
      OverfastApiEndpointType overfastApiEndpointType, String key, Class<T> classType) {
    Cache<String, Object> cache = caches.get(overfastApiEndpointType);
    if (cache == null) return null;
    Object value = cache.getIfPresent(key);
    return classType.isInstance(value) ? (T) value : null;
  }

  public void invalidate(OverfastApiEndpointType overfastApiEndpointType, String key) {
    Cache<String, Object> cache = caches.get(overfastApiEndpointType);
    if (cache != null) cache.invalidate(key);
  }
}
