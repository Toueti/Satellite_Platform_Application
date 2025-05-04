package com.enit.satellite_platform.modules.resource_management.utils.storage_management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TemporaryFileMetadataService {

    private static final Logger log = LoggerFactory.getLogger(TemporaryFileMetadataService.class);

    private final RedisTemplate<String, Object> redisTemplate; // Use Object for flexibility, cast later
    private final String redisPrefix;
    private final Duration inactivityDuration;
    private final Duration postAccessDuration;

    public TemporaryFileMetadataService(
            RedisTemplate<String, Object> redisTemplate,
            @Value("${storage.temporary.redis-prefix}") String redisPrefix,
            @Value("${storage.temporary.inactivity-duration}") Duration inactivityDuration,
            @Value("${storage.temporary.post-access-duration}") Duration postAccessDuration) {
        this.redisTemplate = redisTemplate;
        this.redisPrefix = redisPrefix;
        this.inactivityDuration = inactivityDuration;
        this.postAccessDuration = postAccessDuration;
        log.info("TemporaryFileMetadataService initialized with prefix: {}, inactivity: {}, post-access: {}",
                 redisPrefix, inactivityDuration, postAccessDuration);
    }

    private String buildKey(String fileId) {
        return redisPrefix + fileId;
    }

    public void storeMetadata(String fileId, TemporaryFileMetadata metadata) {
        String key = buildKey(fileId);
        try {
            // Set a Redis TTL slightly longer than the max possible lifetime as a fallback
            long maxLifetimeSeconds = Math.max(inactivityDuration.getSeconds(), postAccessDuration.getSeconds()) + inactivityDuration.getSeconds(); // Generous buffer
            redisTemplate.opsForValue().set(key, metadata, maxLifetimeSeconds, TimeUnit.SECONDS);
            log.debug("Stored metadata for key: {} with TTL: {} seconds", key, maxLifetimeSeconds);
        } catch (Exception e) {
            log.error("Failed to store metadata for key: {}", key, e);
            // Consider re-throwing or handling more gracefully depending on requirements
            throw new RuntimeException("Failed to store temporary file metadata in Redis", e);
        }
    }

    public Optional<TemporaryFileMetadata> getMetadata(String fileId) {
        String key = buildKey(fileId);
        try {
            Object rawValue = redisTemplate.opsForValue().get(key);
            if (rawValue instanceof TemporaryFileMetadata) {
                log.debug("Retrieved metadata for key: {}", key);
                return Optional.of((TemporaryFileMetadata) rawValue);
            } else if (rawValue != null) {
                log.warn("Retrieved unexpected type for key {}: {}", key, rawValue.getClass().getName());
                // Attempt to delete potentially corrupted data
                redisTemplate.delete(key);
                return Optional.empty();
            } else {
                log.debug("No metadata found for key: {}", key);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to retrieve metadata for key: {}", key, e);
            return Optional.empty();
        }
    }

    public void updateLastAccessTime(String fileId) {
        String key = buildKey(fileId);
        try {
            // Use check-and-set (CAS) via opsForValue().getAndSet() or transactions if atomicity is critical
            // For simplicity here, we retrieve, update, and set back.
            Optional<TemporaryFileMetadata> existingMetadataOpt = getMetadata(fileId);
            if (existingMetadataOpt.isPresent()) {
                TemporaryFileMetadata metadata = existingMetadataOpt.get();
                metadata.setLastAccessTime(Instant.now());
                // Re-set the value, potentially updating the TTL if needed, though cleanup service is primary
                long remainingTtl = Optional.ofNullable(redisTemplate.getExpire(key, TimeUnit.SECONDS)).orElse(postAccessDuration.getSeconds());
                redisTemplate.opsForValue().set(key, metadata, remainingTtl, TimeUnit.SECONDS);
                log.debug("Updated last access time for key: {}", key);
            } else {
                log.warn("Attempted to update last access time for non-existent key: {}", key);
            }
        } catch (Exception e) {
            log.error("Failed to update last access time for key: {}", key, e);
        }
    }

    public boolean deleteMetadata(String fileId) {
        String key = buildKey(fileId);
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Deleted metadata for key: {}", key);
                return true;
            } else {
                log.debug("Attempted to delete non-existent metadata key: {}", key);
            }
        } catch (Exception e) {
            log.error("Failed to delete metadata for key: {}", key, e);
        }
        return false; // Return false if deletion failed or key didn't exist
    }

    public Set<String> getAllMetadataFileIds() {
        try {
            Set<String> keys = redisTemplate.keys(redisPrefix + "*");
            if (keys == null) {
                return Set.of();
            }
            // Strip the prefix to get just the file IDs
            return keys.stream()
                       .map(key -> key.substring(redisPrefix.length()))
                       .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to retrieve keys with prefix: {}", redisPrefix, e);
            return Set.of();
        }
    }

    // Getters for durations needed by the cleanup service
    public Duration getInactivityDuration() {
        return inactivityDuration;
    }

    public Duration getPostAccessDuration() {
        return postAccessDuration;
    }
}
