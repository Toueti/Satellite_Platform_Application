package com.enit.satellite_platform.config.cache_handler;

import com.enit.satellite_platform.config.cache_handler.general_cache_handler.CacheHandler;
import com.enit.satellite_platform.config.cache_handler.general_cache_handler.ICacheKeyGenerator;
import com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.openCV.vegetation_Index_calculation.dto.VegetationIndexResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * A specific implementation of {@link CacheHandler} tailored for caching
 * vegetation index calculation results ({@link VegetationIndexResult}).
 *
 * This handler uses Redis as the primary cache. Since the results are computed
 * by an external service and not persisted in a dedicated database table managed
 * by this service, the persistent storage methods are implemented as no-ops.
 *
 * @see CacheHandler
 * @see VegetationIndexResult
 * @see CacheProperties
 * @see ICacheKeyGenerator
 */
@Component
public class VegetationIndexCacheHandler extends CacheHandler<VegetationIndexResult> {

    private static final Logger log = LoggerFactory.getLogger(VegetationIndexCacheHandler.class);

    /**
     * Constructs a new VegetationIndexCacheHandler with injected dependencies.
     * Passes the core cache components (RedisTemplate, KeyGenerator, Properties) to the superclass constructor.
     *
     * @param redisTemplate     The configured {@link RedisTemplate} for Redis operations.
     * @param cacheProperties   The {@link CacheProperties} holding cache configuration (TTL, prefix, etc.).
     * @param cacheKeyGenerator The {@link ICacheKeyGenerator} implementation for creating cache keys.
     */
    public VegetationIndexCacheHandler(
            RedisTemplate<String, Object> redisTemplate,
            CacheProperties cacheProperties,
            ICacheKeyGenerator cacheKeyGenerator) {
        super(redisTemplate, cacheKeyGenerator, cacheProperties);
        log.info("Vegetation Index Cache Handler initialized.");
    }

    /**
     * Attempts to find the result in persistent storage.
     * Since vegetation index results are computed externally and not stored
     * persistently by this handler, this method always returns empty.
     *
     * @param cacheKey The cache key (ignored).
     * @return Always returns {@link Optional#empty()}.
     */
    @Override
    protected Optional<VegetationIndexResult> findInPersistentStorage(String cacheKey) {
        log.debug("Persistent storage lookup skipped for vegetation index result (key: {})", cacheKey);
        // Results are computed externally, no persistent storage lookup needed here.
        return Optional.empty();
    }

    /**
     * Saves the result to persistent storage.
     * Since vegetation index results are computed externally and not stored
     * persistently by this handler, this method is a no-op.
     *
     * @param data     The {@link VegetationIndexResult} object (ignored).
     * @param cacheKey The cache key associated with the data (ignored).
     */
    @Override
    protected void saveToStorage(VegetationIndexResult data, String cacheKey) {
        log.debug("Persistent storage save skipped for vegetation index result (key: {})", cacheKey);
        // Results are computed externally, no persistent storage save needed here.
    }

    // No scheduled cleanup needed specifically for this handler,
    // as Redis TTL handles expiration. The generic cleanup in CacheHandler
    // might still run if configured, but won't find persistent entries to clean for this type.
}
