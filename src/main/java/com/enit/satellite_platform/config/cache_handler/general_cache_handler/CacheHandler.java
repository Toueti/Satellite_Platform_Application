package com.enit.satellite_platform.config.cache_handler.general_cache_handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;


import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * A generic cache handler that manages resources using Redis for caching
 * and a persistent storage backend.
 *
 * @param <T> The type of data being cached
 * It interacts with the {@link CacheKeyGenerator} for key generation,
 * uses a generic {@link RedisTemplate} for cache operations and
 * {@link CachePropertiesBase} for configuration properties.
 */
@Component
public abstract class CacheHandler<T> {

    private static final Logger log = LoggerFactory.getLogger(CacheHandler.class);

    // Use the generic RedisTemplate
    private final RedisTemplate<String, Object> redisTemplate;
    private final ICacheKeyGenerator cacheKeyGenerator; // Use interface for DI
    private final CachePropertiesBase cacheProperties; // Use CacheProperties for DI

    /**
     * Constructor for the GenericCacheHandler.
     *
     * @param redisTemplate     The generic Redis template for cache operations
     * @param cacheKeyGenerator The key generator implementation (using interface type)
     * @param cacheProperties   Cache configuration properties
     */
    public CacheHandler(RedisTemplate<String, Object> redisTemplate, // Accept generic template
                        ICacheKeyGenerator cacheKeyGenerator,
                        CachePropertiesBase cacheProperties) {
        this.cacheProperties = cacheProperties;
        this.redisTemplate = redisTemplate;
        this.cacheKeyGenerator = cacheKeyGenerator;
    }

    /**
     * Implements Cache Read Flow (Step 1).
     * Attempts to retrieve data first from Redis cache, then from
     * the persistent storage.
     * If found in persistent storage but not Redis, it caches the result in Redis before
     * returning.
     *
     * @param object object to generate cache key.
     *               This could be a combination of identifiers or a single object.
     * @return Optional containing the data if found, otherwise empty.
     */
    public Optional<T> getResourceData(Object object) {
        String cacheKey = cacheKeyGenerator.generateKey(object);
        log.debug("Attempting to retrieve data with cache key: {}", cacheKey);

        // 1. Check Redis first
        Optional<CacheEntry<T>> cachedEntry = getFromRedis(cacheKey);
        if (cachedEntry.isPresent()) {
            log.info("Cache hit for key: {}", cacheKey);
            CacheEntry<T> entry = cachedEntry.get();
            entry.recordAccess();
            // Resave entry to update metadata and reset TTL
            storeInRedis(cacheKey, entry);
            return Optional.of(entry.getData());
        }

        log.info("Cache miss for key: {}. Checking persistent storage.", cacheKey);

        // 2. If cache miss, check persistent storage using the abstract method
        Optional<T> persistentResult = findInPersistentStorage(cacheKey);

        if (persistentResult.isPresent()) {
            log.info("Data found in persistent storage for key: {}. Caching in Redis.", cacheKey);
            // 3. Store in Redis before returning (Wrap in CacheEntry)
            storeInRedis(cacheKey, new CacheEntry<>(persistentResult.get()));
            return persistentResult;
        } else {
            log.info("Data not found in persistent storage for key: {}.", cacheKey);
            // 4. If not found in both, return empty Optional
            return Optional.empty();
        }
    }

    /**
     * Implements Cache Write Flow.
     * Stores the given data in the persistent storage (optional) and Redis cache.
     *
     * @param data   The data to store
     * @param object The object to generate cache key
     * @param persistToPermanentStorage Whether to also save to persistent storage
     * @return The generated cache key
     */
    public String storeResourceData(T data, Object object, boolean persistToPermanentStorage) {
        if (data == null) {
            log.warn("Attempted to store null data. Returning null key.");
            return null;
        }

        String cacheKey = cacheKeyGenerator.generateKey(object);
        log.debug("Storing data with cache key: {}", cacheKey);

        // Optionally store in persistent storage
        if (persistToPermanentStorage) {
            saveToStorage(data, cacheKey);
        }

        // Store in Redis (Cache with TTL) - Wrap in CacheEntry
        storeInRedis(cacheKey, new CacheEntry<>(data));

        return cacheKey;
    }

    /**
     * Overloaded method that defaults to not persisting to permanent storage.
     */
    public String storeResourceData(T data, Object object) {
        return storeResourceData(data, object, false);
    }

    /**
     * Implements explicit cache invalidation.
     * Removes the cached entry from Redis.
     *
     * @param object The object used to generate the cache key
     */
    public void invalidateCache(Object object) {
        String cacheKey = cacheKeyGenerator.generateKey(object);
        log.debug("Invalidating cache for key: {}", cacheKey);
        deleteFromRedis(cacheKey);
    }

    /**
     * Abstract method to find data in the persistent storage.
     * Implementations should define how to retrieve data from their specific storage.
     *
     * @param cacheKey The cache key to look up
     * @return Optional containing the data if found
     */
    protected abstract Optional<T> findInPersistentStorage(String cacheKey);

    /**
     * Abstract method to save data to the persistent storage.
     * Implementations should define how to save data to their specific storage.
     *
     * @param data The data to save
     * @param cacheKey The cache key to associate with the data
     */
    protected abstract void saveToStorage(T data, String cacheKey);

    // --- Helper Methods ---

    /**
     * Retrieves a CacheEntry from Redis.
     *
     * @param key The cache key
     * @return Optional containing the CacheEntry if found
     */
    @SuppressWarnings("unchecked") // Suppress warning for the necessary cast
    private Optional<CacheEntry<T>> getFromRedis(String key) {
        try {
            Object rawValue = redisTemplate.opsForValue().get(key);
            if (rawValue instanceof CacheEntry) {
                // Explicit cast needed as template returns Object
                CacheEntry<T> entry = (CacheEntry<T>) rawValue;
                return Optional.of(entry);
            } else if (rawValue != null) {
                // Log if the retrieved object is not of the expected type
                log.warn("Retrieved object from Redis for key '{}' is not of type CacheEntry. Type: {}", key, rawValue.getClass().getName());
                return Optional.empty();
            }
            return Optional.empty(); // Key not found or value is null
        } catch (ClassCastException e) {
            log.error("Error casting retrieved object to CacheEntry for key '{}': {}", key, e.getMessage(), e);
            // Optionally delete the problematic key
            // deleteFromRedis(key);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting value from Redis for key '{}': {}", key, e.getMessage(), e);
            return Optional.empty(); // Treat Redis error as cache miss
        }
    }

    /**
     * Stores a CacheEntry in Redis with the configured TTL.
     *
     * @param key   The cache key
     * @param entry The CacheEntry to store
     */
    private void storeInRedis(String key, CacheEntry<T> entry) {
        try {
            // Use the injected TTL value. The generic template accepts Object as value.
            redisTemplate.opsForValue().set(key, entry, cacheProperties.getRedisTtlSeconds(), TimeUnit.SECONDS);
            log.debug("Stored/Updated CacheEntry in Redis with key '{}' and TTL {} seconds. Access count: {}", key,
                    cacheProperties.getRedisTtlSeconds(), entry.getAccessCount());
        } catch (Exception e) {
            log.error("Error storing CacheEntry in Redis for key '{}': {}", key, e.getMessage(), e);
        }
    }

    /**
     * Deletes a key from Redis.
     * 
     * @param key The cache key to delete
     */
    private void deleteFromRedis(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Deleted data from Redis with key '{}'", key);
        } catch (Exception e) {
            log.error("Error deleting data from Redis for key '{}': {}", key, e.getMessage(), e);
        }
    }


    @SuppressWarnings("unchecked") // Suppress warning for the necessary cast
    public void cleanInfrequentlyUsedCache() {
        log.info("Starting scheduled cache cleanup for infrequently used entries...");
        long cleanedCount = 0;
        // Use injected inactivity threshold
        Instant cutoffTime = Instant.now().minus(Duration.ofDays(cacheProperties.getInactivityThresholdDays()));

        // Use injected prefix
        ScanOptions options = ScanOptions.scanOptions().match(cacheProperties.getCachePrefix() + "*").count(100).build();
        List<String> keysToDelete = new ArrayList<>();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                try {
                    Object rawValue = redisTemplate.opsForValue().get(key);
                    if (rawValue instanceof CacheEntry) {
                        CacheEntry<T> entry = (CacheEntry<T>) rawValue; // Cast needed
                        // Use injected max access count
                        if (entry.getAccessCount() <= cacheProperties.getMaxInfrequentAccessCount() &&
                                entry.getLastAccessed().isBefore(cutoffTime)) {
                            keysToDelete.add(key);
                        }
                    } else if (rawValue != null) {
                        log.warn("Found non-CacheEntry object during cleanup scan for key '{}'. Type: {}", key, rawValue.getClass().getName());
                    }
                } catch (ClassCastException e) {
                    log.error("Error casting object during cache cleanup scan for key '{}': {}", key, e.getMessage(), e);
                } catch (Exception e) {
                    log.error("Error processing key '{}' during cache cleanup scan: {}", key, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error during Redis SCAN operation for cache cleanup: {}", e.getMessage(), e);
            return;
        }

        if (!keysToDelete.isEmpty()) {
            try {
                Long deletedCount = redisTemplate.delete(keysToDelete);
                cleanedCount = deletedCount != null ? deletedCount : 0;
                log.info("Successfully deleted {} infrequently used/old cache entries.", cleanedCount);
            } catch (Exception e) {
                log.error("Error batch deleting keys during cache cleanup: {}", e.getMessage(), e);
            }
        } else {
            log.info("No infrequently used/old cache entries found to clean.");
        }

        log.info("Finished scheduled cache cleanup.");
    }
}
