package com.enit.satellite_platform.config.cache_handler.general_cache_handler;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents a cached item with metadata for cache management.
 * 
 * @param <T> The type of data being cached
 */
public class CacheEntry<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final T data;
    private int accessCount;
    private Instant lastAccessed;
    private final Instant createdAt;
    
    /**
     * Creates a new cache entry with the provided data.
     * 
     * @param data The data to cache
     */
    public CacheEntry(T data) {
        this.data = data;
        this.accessCount = 1;
        this.lastAccessed = Instant.now();
        this.createdAt = Instant.now();
    }
    
    /**
     * Records an access to this cache entry.
     * Increments the access count and updates the last accessed timestamp.
     */
    public void recordAccess() {
        this.accessCount++;
        this.lastAccessed = Instant.now();
    }
    
    /**
     * Gets the cached data.
     * 
     * @return The cached data
     */
    public T getData() {
        return data;
    }
    
    /**
     * Gets the number of times this entry has been accessed.
     * 
     * @return The access count
     */
    public int getAccessCount() {
        return accessCount;
    }
    
    /**
     * Gets the time this entry was last accessed.
     * 
     * @return The last accessed timestamp
     */
    public Instant getLastAccessed() {
        return lastAccessed;
    }
    
    /**
     * Gets the time this entry was created.
     * 
     * @return The creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
}