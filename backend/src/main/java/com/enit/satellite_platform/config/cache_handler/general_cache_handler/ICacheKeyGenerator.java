package com.enit.satellite_platform.config.cache_handler.general_cache_handler;

/**
 * Interface for generating cache keys from objects.
 */
public interface ICacheKeyGenerator {
    
    /**
     * Generates a cache key from the given object.
     * 
     * @param object The object to generate a key for
     * @return The generated cache key
     */
    String generateKey(Object object);
}