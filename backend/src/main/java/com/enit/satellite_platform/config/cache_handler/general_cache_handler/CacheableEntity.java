package com.enit.satellite_platform.config.cache_handler.general_cache_handler;

public interface CacheableEntity {
    String getCacheKey();
    void setCacheKey(String cacheKey);
}
// This interface defines the contract for cacheable entities, ensuring they have a cache key for identification.
