package com.enit.satellite_platform.config.cache_handler.general_cache_handler;

import java.io.Serializable;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A wrapper class for cached data that includes metadata for cache management purposes,
 * such as access count and timestamps. This class is designed to be serializable for storage
 * in caches like Redis. It uses Jackson annotations for proper serialization/deserialization.
 *
 * @param <T> The type of the actual data being cached. Must be serializable.
 */
public class CacheEntry<T> implements Serializable {
    private static final long serialVersionUID = 1L; // Standard practice for Serializable classes

    /** The actual data being cached. */
    private final T data;
    /** Counter for how many times this cache entry has been accessed. */
    private int accessCount;
    /** Timestamp of the last time this entry was accessed. */
    private Instant lastAccessed;
    /** Timestamp of when this cache entry was initially created. */
    private final Instant createdAt;

    /**
     * Private no-argument constructor required by Jackson for deserialization.
     * Initializes fields to default values; should not be used directly.
     */
    @SuppressWarnings("unused")
    private CacheEntry() {
        this.data = null; // Or handle appropriately if null is not allowed
        this.accessCount = 0;
        this.lastAccessed = null;
        this.createdAt = null;
    }

    /**
     * Creates a new CacheEntry instance wrapping the given data.
     * Initializes metadata: sets access count to 1, and timestamps to the current time.
     * {@code @JsonCreator} and {@code @JsonProperty} are used to help Jackson correctly
     * instantiate this object during deserialization from JSON.
     *
     * @param data The actual data object to be cached.
     */
    @JsonCreator
    public CacheEntry(@JsonProperty("data") T data) {
        this.data = data;
        this.accessCount = 1; // Initial access
        this.lastAccessed = Instant.now();
        this.createdAt = Instant.now();
    }

    /**
     * Updates the metadata to reflect that the cache entry has been accessed.
     * Specifically, it increments the {@link #accessCount} and sets the
     * {@link #lastAccessed} timestamp to the current time ({@code Instant.now()}).
     * This method should be called whenever the cached data is retrieved and used.
     */
    public void recordAccess() {
        this.accessCount++;
        this.lastAccessed = Instant.now();
    }

    /**
     * Returns the underlying cached data object.
     *
     * @return The actual data of type {@code T} stored in this entry.
     */
    public T getData() {
        return data;
    }

    /**
     * Returns the number of times this cache entry has been accessed since creation or last reset.
     * This count is incremented by {@link #recordAccess()}.
     *
     * @return The current access count.
     */
    public int getAccessCount() {
        return accessCount;
    }

    /**
     * Returns the timestamp indicating the last time this cache entry was accessed.
     * This timestamp is updated by the {@link #recordAccess()} method.
     *
     * @return The {@link Instant} representing the last access time.
     */
    public Instant getLastAccessed() {
        return lastAccessed;
    }

    /**
     * Returns the timestamp indicating when this cache entry was originally created.
     * This timestamp is set once during object construction and does not change.
     *
     * @return The {@link Instant} representing the creation time.
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
