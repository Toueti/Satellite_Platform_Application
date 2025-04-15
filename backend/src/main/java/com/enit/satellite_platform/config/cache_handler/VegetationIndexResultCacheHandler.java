package com.enit.satellite_platform.config.cache_handler;

import com.enit.satellite_platform.config.cache_handler.general_cache_handler.ICacheKeyGenerator;
import com.enit.satellite_platform.config.cache_handler.util.FileHashingUtil;
import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.openCV.vegetation_Index_calculation.dto.VegetationIndexResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.enit.satellite_platform.config.cache_handler.general_cache_handler.CacheEntry; // Added import

import java.util.Optional;

/**
 * Specialized cache handler for VegetationIndexResult that includes file content-based caching.
 * This handler takes into account both the metadata and the image data when generating cache keys
 * and storing results.
 */
@Component
public class VegetationIndexResultCacheHandler extends FileBasedCacheHandler<VegetationIndexResult> {
    private static final Logger log = LoggerFactory.getLogger(VegetationIndexResultCacheHandler.class);

    public VegetationIndexResultCacheHandler(
            RedisTemplate<String, Object> redisTemplate,
            ICacheKeyGenerator cacheKeyGenerator,
            CacheProperties cacheProperties,
            FileHashingUtil fileHashingUtil) {
        super(redisTemplate, cacheKeyGenerator, cacheProperties, fileHashingUtil);
    }

    @Override
    protected String calculateFileContentHash(VegetationIndexResult data) {
        if (data == null || data.getProcessedImage() == null) {
            return "no-image";
        }
        return fileHashingUtil.calculateHash(data.getProcessedImage());
    }

    @Override
    protected Optional<VegetationIndexResult> findInPersistentStorage(String cacheKey) {
        log.debug("Persistent storage lookup skipped for vegetation index result (key: {})", cacheKey);
        // Results are computed externally, no persistent storage lookup needed
        return Optional.empty();
    }

    @Override
    protected void saveToStorage(VegetationIndexResult data, String cacheKey) {
        log.debug("Persistent storage save skipped for vegetation index result (key: {})", cacheKey);
        // Results are computed externally, no persistent storage save needed
    }

    @Override
    public String storeResourceData(VegetationIndexResult data, Object object) {
        if (data == null) {
            log.warn("Attempted to store null data. Returning null key.");
            return null;
        }

        // Generate key only from the input parameters (object contains request + input image hash)
        String key = cacheKeyGenerator.generateKey(object);
        log.debug("Storing vegetation index result with input-based key: {}", key);

        // Directly store in Redis using the correctly generated input-based key
        // Bypass the base class storeResourceData to avoid key regeneration issues.
        // We need access to the private storeInRedis method or make it protected.
        // For now, let's assume we modify CacheHandler to make storeInRedis protected.
        storeInRedis(key, new CacheEntry<>(data)); // Assuming storeInRedis is made protected

        return key;
    }
}
