package com.enit.satellite_platform.config.cache_handler.general_cache_handler;

/**
 * Defines the contract for generating unique string keys used for caching.
 * Implementations of this interface determine how an input object (which could represent
 * request parameters, entity IDs, or other identifying information) is converted into
 * a suitable key for storage in a cache like Redis.
 *
 * The generated key should ideally include a prefix specific to the type of data being cached
 * to avoid collisions between different types of cached objects.
 *
 * @see CacheHandler
 */
public interface ICacheKeyGenerator {

    /**
     * Generates a unique string representation (cache key) based on the provided object.
     * The implementation should ensure that the same logical input object consistently
     * produces the same output key. The key should be suitable for use in the underlying
     * cache store (e.g., Redis key naming conventions).
     *
     * @param object The input object containing information to derive the cache key from.
     *               This could be a simple ID, a complex DTO, or any object representing
     *               the resource to be cached or retrieved.
     * @return A non-null, unique string representing the cache key for the given object.
     */
    String generateKey(Object object);
}
