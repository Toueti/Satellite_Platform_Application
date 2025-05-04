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
 * Abstract base class for implementing a cache-aside strategy with Redis and a persistent backend.
 * This class provides the core logic for retrieving, storing, and invalidating cached data,
 * while delegating persistent storage operations to concrete subclasses.
 * It uses {@link CacheEntry} to wrap cached data with metadata (access count, timestamp).
 *
 * @param <T> The type of data being cached. This type should be serializable for Redis storage.
 *
 * @see CacheEntry
 * @see ICacheKeyGenerator
 * @see CachePropertiesBase
 * @see RedisTemplate
 */
@Component
public abstract class CacheHandler<T> {

    private static final Logger log = LoggerFactory.getLogger(CacheHandler.class);

    // Use the generic RedisTemplate
    private final RedisTemplate<String, Object> redisTemplate;
    protected final ICacheKeyGenerator cacheKeyGenerator; // Use interface for DI
    protected final CachePropertiesBase cacheProperties; // Use base class for properties

    /**
     * Constructs a new CacheHandler. Dependencies are typically injected by a framework like Spring.
     *
     * @param redisTemplate     The configured {@link RedisTemplate} for Redis operations (typically {@code RedisTemplate<String, Object>}).
     * @param cacheKeyGenerator The {@link ICacheKeyGenerator} implementation used to generate keys for cache entries.
     * @param cacheProperties   The {@link CachePropertiesBase} implementation containing cache configuration (TTL, prefix, cleanup settings).
     */
    public CacheHandler(RedisTemplate<String, Object> redisTemplate,
                        ICacheKeyGenerator cacheKeyGenerator,
                        CachePropertiesBase cacheProperties) {
        this.cacheProperties = cacheProperties;
        this.redisTemplate = redisTemplate;
        this.cacheKeyGenerator = cacheKeyGenerator;
    }

    /**
     * Retrieves resource data based on the provided object, implementing the cache-aside pattern.
     * 1. Generates a cache key using {@link ICacheKeyGenerator}.
     * 2. Attempts to fetch the {@link CacheEntry} from Redis.
     * 3. If found (cache hit), updates access metadata, resets TTL in Redis, and returns the data.
     * 4. If not found (cache miss), calls the abstract {@link #findInPersistentStorage(String)} method.
     * 5. If found in persistent storage, wraps the data in a {@link CacheEntry}, stores it in Redis, and returns the data.
     * 6. If not found in persistent storage, returns {@link Optional#empty()}.
     *
     * @param object The object used to generate the cache key (e.g., an ID, a request DTO).
     * @return An {@link Optional} containing the requested data (type {@code T}) if found in cache or persistent storage,
     *         otherwise an empty Optional.
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
     * Stores or updates resource data in the cache and optionally in persistent storage.
     * 1. Generates a cache key using {@link ICacheKeyGenerator}.
     * 2. If {@code persistToPermanentStorage} is true, calls the abstract {@link #saveToStorage(T, String)} method.
     * 3. Wraps the data in a {@link CacheEntry}.
     * 4. Stores the {@link CacheEntry} in Redis using the configured TTL.
     *
     * @param data                      The resource data (type {@code T}) to store. Must not be null.
     * @param object                    The object used to generate the cache key.
     * @param persistToPermanentStorage If true, the data will also be saved via {@link #saveToStorage(T, String)}.
     * @return The generated cache key, or null if the input data was null.
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
     * Stores or updates resource data in the cache only (does not persist to permanent storage).
     * This is an overloaded version of {@link #storeResourceData(T, Object, boolean)} with
     * {@code persistToPermanentStorage} set to false.
     *
     * @param data   The resource data (type {@code T}) to store. Must not be null.
     * @param object The object used to generate the cache key.
     * @return The generated cache key, or null if the input data was null.
     */
    public String storeResourceData(T data, Object object) {
        return storeResourceData(data, object, false);
    }

    /**
     * Explicitly removes an entry from the Redis cache.
     * Generates the cache key from the provided object and deletes it from Redis.
     * This does not affect the data in persistent storage.
     *
     * @param object The object used to generate the cache key for the entry to invalidate.
     */
    public void invalidateCache(Object object) {
        String cacheKey = cacheKeyGenerator.generateKey(object);
        log.debug("Invalidating cache for key: {}", cacheKey);
        deleteFromRedis(cacheKey);
    }

    /**
     * Abstract method to be implemented by subclasses to define how to retrieve data
     * from the specific persistent storage backend (e.g., database, file system, external API).
     * This method is called during a cache miss.
     *
     * @param cacheKey The cache key associated with the data to find. Subclasses may need to parse
     *                 or use this key to query their backend.
     * @return An {@link Optional} containing the data (type {@code T}) if found in persistent storage,
     *         otherwise an empty Optional.
     */
    protected abstract Optional<T> findInPersistentStorage(String cacheKey);

    /**
     * Abstract method to be implemented by subclasses to define how to save or update data
     * in the specific persistent storage backend.
     * This method is called by {@link #storeResourceData(T, Object, boolean)} when
     * {@code persistToPermanentStorage} is true.
     *
     * @param data     The data (type {@code T}) to save or update in persistent storage.
     * @param cacheKey The cache key associated with the data. Subclasses might use this key
     *                 to determine the storage location or identifier.
     */
    protected abstract void saveToStorage(T data, String cacheKey);

    // --- Helper Methods ---

    /**
     * Retrieves a CacheEntry from Redis. Handles potential ClassCastException if the stored
     * object is not of the expected type.
     *
     * @param key The cache key
     * @return Optional containing the CacheEntry if found and of the correct type
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
            // Optionally delete the problematic key if it's corrupted
            // deleteFromRedis(key);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting value from Redis for key '{}': {}", key, e.getMessage(), e);
            return Optional.empty(); // Treat Redis error as cache miss
        }
    }

    /**
     * Stores a CacheEntry in Redis with the configured TTL.
     * Logs the operation details including TTL and access count.
     *
     * @param key   The cache key
     * @param entry The CacheEntry to store
     */
    protected void storeInRedis(String key, CacheEntry<T> entry) { // Changed from private to protected
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
     * Deletes a key from Redis. Logs the operation.
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

    /**
     * Performs cache cleanup by scanning Redis keys matching the configured prefix.
     * It identifies entries that are considered infrequently accessed (based on access count
     * and last accessed time thresholds from {@link CachePropertiesBase}) and deletes them.
     * Uses Redis SCAN command for efficient iteration over keys without blocking the server.
     * Handles potential errors during scanning and deletion gracefully.
     */
    @SuppressWarnings("unchecked") // Suppress warning for the necessary cast
    public void cleanInfrequentlyUsedCache() {
        log.info("Starting scheduled cache cleanup for infrequently used entries...");
        long cleanedCount = 0;
        // Use injected inactivity threshold
        Instant cutoffTime = Instant.now().minus(Duration.ofDays(cacheProperties.getInactivityThresholdDays()));

        // Use injected prefix and configure SCAN options
        ScanOptions options = ScanOptions.scanOptions().match(cacheProperties.getCachePrefix() + "*").count(100).build(); // Adjust count as needed
        List<String> keysToDelete = new ArrayList<>();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                try {
                    Object rawValue = redisTemplate.opsForValue().get(key);
                    if (rawValue instanceof CacheEntry) {
                        CacheEntry<T> entry = (CacheEntry<T>) rawValue; // Cast needed
                        // Check against configured thresholds
                        if (entry.getAccessCount() <= cacheProperties.getMaxInfrequentAccessCount() &&
                                entry.getLastAccessed().isBefore(cutoffTime)) {
                            keysToDelete.add(key);
                            log.trace("Marking key '{}' for cleanup (Access Count: {}, Last Accessed: {})", key, entry.getAccessCount(), entry.getLastAccessed());
                        }
                    } else if (rawValue != null) {
                        log.warn("Found non-CacheEntry object during cleanup scan for key '{}'. Type: {}", key, rawValue.getClass().getName());
                        // Optionally delete invalid entries: keysToDelete.add(key);
                    }
                } catch (ClassCastException e) {
                    log.error("Error casting object during cache cleanup scan for key '{}': {}", key, e.getMessage(), e);
                    // Optionally delete invalid entries: keysToDelete.add(key);
                } catch (Exception e) {
                    // Log error for the specific key but continue scanning
                    log.error("Error processing key '{}' during cache cleanup scan: {}", key, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            // Log error for the SCAN operation itself and abort cleanup
            log.error("Error during Redis SCAN operation for cache cleanup: {}", e.getMessage(), e);
            return; // Abort cleanup if SCAN fails
        }

        // Batch delete the identified keys
        if (!keysToDelete.isEmpty()) {
            try {
                log.info("Attempting to delete {} infrequently used/old cache entries...", keysToDelete.size());
                Long deletedCount = redisTemplate.delete(keysToDelete);
                cleanedCount = deletedCount != null ? deletedCount : 0;
                log.info("Successfully deleted {} infrequently used/old cache entries.", cleanedCount);
            } catch (Exception e) {
                log.error("Error batch deleting keys during cache cleanup: {}", e.getMessage(), e);
            }
        } else {
            log.info("No infrequently used/old cache entries found matching the criteria.");
        }

        log.info("Finished scheduled cache cleanup.");
    }
}
