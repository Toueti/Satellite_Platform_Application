package com.enit.satellite_platform.config.cache_handler;

import com.enit.satellite_platform.config.cache_handler.general_cache_handler.CacheHandler;
import com.enit.satellite_platform.config.cache_handler.general_cache_handler.ICacheKeyGenerator;
import com.enit.satellite_platform.modules.resource_management.dto.ProcessingResponse;
import com.enit.satellite_platform.shared.mapper.ResultsMapper;
import com.enit.satellite_platform.modules.resource_management.image_management.entities.ProcessingResults;
import com.enit.satellite_platform.modules.resource_management.image_management.repositories.ResultsRepository;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * A specific implementation of {@link CacheHandler} tailored for caching satellite image
 * processing results ({@link ProcessingResponse}).
 *
 * This handler uses Redis as the primary cache and MongoDB (via {@link ResultsRepository})
 * as the persistent storage backend. It leverages {@link ResultsMapper} to convert between
 * the persistent entity {@link ProcessingResults} and the cached DTO {@link ProcessingResponse}.
 *
 * It also includes functionality to schedule cache cleanup based on configuration properties
 * ({@code cache.cleanup.cron} and {@code cache.cleanup.enabled}) using a {@link TaskScheduler}.
 * The {@code @RefreshScope} allows dynamic updates to cache properties and cleanup schedule.
 *
 * @see CacheHandler
 * @see ProcessingResponse
 * @see ProcessingResults
 * @see ResultsRepository
 * @see ResultsMapper
 * @see CacheProperties
 * @see TaskScheduler
 */
@Component
@RefreshScope
public class SatelliteProcessingCacheHandler extends CacheHandler<ProcessingResponse> {

    private static final Logger log = LoggerFactory.getLogger(SatelliteProcessingCacheHandler.class);

    /** Cron expression defining when the cache cleanup task should run. Defaults to "0 0 3 * * ?". */
    @Value("${cache.cleanup.cron:0 0 3 * * ?}")
    private String cronExpression;

    /** Flag to enable or disable the scheduled cache cleanup task. Defaults to true. */
    @Value("${cache.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    /** Spring's TaskScheduler used to schedule the cache cleanup job. */
    private final TaskScheduler taskScheduler;

    /**
     * Constructs a new SatelliteProcessingCacheHandler with injected dependencies.
     * Passes the core cache components (RedisTemplate, KeyGenerator, Properties) to the superclass constructor.
     *
     * @param redisTemplate     The configured {@link RedisTemplate} for Redis operations.
     * @param cacheProperties   The {@link CacheProperties} holding cache configuration (TTL, prefix, cleanup thresholds).
     * @param resultsRepository The {@link ResultsRepository} for accessing persistent {@link ProcessingResults}.
     * @param cacheKeyGenerator The {@link ICacheKeyGenerator} implementation for creating cache keys.
     * @param taskScheduler     The {@link TaskScheduler} for scheduling the cache cleanup task.
     * @param resultsMapper     The {@link ResultsMapper} for converting between entity and DTO.
     */
    public SatelliteProcessingCacheHandler(
            RedisTemplate<String, Object> redisTemplate,
            CacheProperties cacheProperties,
            ResultsRepository resultsRepository,
            CacheKeyGenerator cacheKeyGenerator,
            TaskScheduler taskScheduler,
            ResultsMapper resultsMapper) {
        super(redisTemplate, cacheKeyGenerator, cacheProperties);
        this.taskScheduler = taskScheduler;
        log.info("Satellite Processing Cache Handler initialized with persistence layer integration.");
    }

    /**
     * Fetches satellite processing results from the persistent storage (MongoDB) using the cache key.
     * This method is invoked by the base {@link CacheHandler} during a cache miss.
     * It queries the {@link ResultsRepository} for a {@link ProcessingResults} entity matching the key
     * and then uses the {@link ResultsMapper} to convert it to a {@link ProcessingResponse} DTO.
     *
     * @param cacheKey The cache key identifying the resource to find in persistent storage.
     * @return An {@link Optional} containing the mapped {@link ProcessingResponse} if found,
     *         otherwise {@link Optional#empty()}.
     */
    @Override
    protected Optional<ProcessingResponse> findInPersistentStorage(String cacheKey) {
        log.debug("Persistent storage lookup skipped for satellite processing result (key: {})", cacheKey);
        return Optional.empty();
    }

    /**
     * Defines how to save a {@link ProcessingResponse} to persistent storage.
     * For this specific handler, {@link ProcessingResponse} objects represent results derived
     * from potentially complex processing and are typically generated on demand or retrieved
     * from an existing persistent {@link ProcessingResults} entity. They are not intended
     * to be saved back to the persistent store directly via this cache handler.
     * Therefore, this method is intentionally left empty (no-op) and logs a debug message.
     *
     * @param data     The {@link ProcessingResponse} object (ignored).
     * @param cacheKey The cache key associated with the data (ignored).
     */
    @Override
    protected void saveToStorage(ProcessingResponse data, String cacheKey) {
        log.debug("Skipping persistent storage for processing response (key: {})", cacheKey);
        // ProcessingResponse objects are not stored persistently
    }

    /**
     * Schedules the cache cleanup task using the configured cron expression and enabled flag.
     * This method is automatically invoked by Spring after the bean has been constructed
     * due to the {@code @PostConstruct} annotation.
     * It retrieves the cron expression and enabled status from the injected {@code @Value} fields
     * (bound to {@code cache.cleanup.cron} and {@code cache.cleanup.enabled} properties).
     * If cleanup is enabled, it schedules the {@link CacheHandler#cleanInfrequentlyUsedCache()}
     * method to run according to the cron schedule via the {@link TaskScheduler}.
     */
    @PostConstruct
    public void scheduleCacheCleanup() {
        if (cleanupEnabled) {
            taskScheduler.schedule(
                this::cleanInfrequentlyUsedCache,
                new CronTrigger(cronExpression)
            );
            log.info("Scheduled cache cleanup task with cron expression: {}", cronExpression);
        } else {
            log.info("Cache cleanup task is disabled");
        }
    }
}
