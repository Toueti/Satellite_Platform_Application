package com.enit.satellite_platform.config.cache_handler;

import org.springframework.data.redis.core.RedisTemplate;

import com.enit.satellite_platform.config.cache_handler.general_cache_handler.CacheHandler;
import com.enit.satellite_platform.config.cache_handler.general_cache_handler.ICacheKeyGenerator;
import com.enit.satellite_platform.config.cache_handler.util.FileHashingUtil;

/**
 * Extension of CacheHandler that provides specialized handling for objects containing file data.
 * Uses FileHashingUtil to generate cache keys that take into account file content.
 * 
 * @param <T> The type of data being cached, expected to contain file data
 */
public abstract class FileBasedCacheHandler<T> extends CacheHandler<T> {
    protected final FileHashingUtil fileHashingUtil;

    public FileBasedCacheHandler(
            RedisTemplate<String, Object> redisTemplate,
            ICacheKeyGenerator cacheKeyGenerator,
            CacheProperties cacheProperties,
            FileHashingUtil fileHashingUtil) {
        super(redisTemplate, cacheKeyGenerator, cacheProperties);
        this.fileHashingUtil = fileHashingUtil;
    }

    /**
     * Method to be implemented by subclasses to calculate a hash of the file content.
     * This hash will be used as part of the cache key to ensure that files with different
     * content but same metadata get different cache entries.
     *
     * @param data The data object containing file content
     * @return A string hash of the file content
     */
    protected abstract String calculateFileContentHash(T data);
}
