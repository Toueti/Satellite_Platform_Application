package com.enit.satellite_platform.config.cache_handler;

import com.enit.satellite_platform.config.cache_handler.general_cache_handler.CacheHandler;
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
 * Handles caching logic for GeeResponse objects using Redis,
 * extending the generic CacheHandler. Does not interact with persistent
 * storage.
 */
@Component
@RefreshScope
public class ProcessingResponseCacheHandler extends CacheHandler<ProcessingResponse> {

    private static final Logger log = LoggerFactory.getLogger(ProcessingResponseCacheHandler.class);
    @Value("${cache.cleanup.cron:0 0 3 * * ?}") 
    private String cronExpression; // Cron expression for scheduling cache cleanup
    private final TaskScheduler taskScheduler;
    private final ResultsRepository resultsRepository;
    private final ResultsMapper resultsMapper; // Added mapper field

    /**
     * Constructor for ProcessingResponseCacheHandler.
     *
     * @param redisTemplate     The generic Redis template (RedisTemplate<String, Object>)
     * @param cacheProperties   Cache configuration properties
     * @param resultsRepository The repository for accessing persistent ProcessingResults data
     * @param cacheKeyGenerator The concrete key generator instance
     * @param resultsMapper     The mapper for converting ProcessingResults to ProcessingResponse
     */
    public ProcessingResponseCacheHandler(RedisTemplate<String, Object> redisTemplate,
                                CacheProperties cacheProperties,
                                ResultsRepository resultsRepository,
                                CacheKeyGenerator cacheKeyGenerator,
                                TaskScheduler taskScheduler,
                                ResultsMapper resultsMapper) { // Added mapper parameter
        super(redisTemplate, cacheKeyGenerator, cacheProperties);
        this.taskScheduler = taskScheduler;
        this.resultsRepository = resultsRepository;
        this.resultsMapper = resultsMapper; // Initialize mapper
        log.info("ProcessingResponseCacheHandler initialized with ResultsRepository and ResultsMapper.");
    }

    /**
     * Finds GeeResponse in persistent storage. Since GeeResponse is transient (from
     * an external service),
     * this method always returns empty.
     *
     * @param cacheKey The cache key (unused in this implementation).
     * @return Always returns Optional.empty().
     */


    @Override
    protected Optional<ProcessingResponse> findInPersistentStorage(String cacheKey) {
        log.debug("Searching persistent storage (MongoDB) for cache key: {}", cacheKey);
        Optional<ProcessingResults> result = resultsRepository.findByCacheKey(cacheKey);
        if (result.isPresent()) {
            log.debug("Found data in MongoDB for cache key: {}", cacheKey);
        } else {
            log.debug("Data not found in MongoDB for cache key: {}", cacheKey);
        }

        // Map ProcessingResults to ProcessingResponse if present
        return result.map(resultsMapper::toProcessingResponse);
    }

    /**
     * Saves GeeResponse to persistent storage. Since GeeResponse is transient,
     * this method does nothing.
     *
     * @param data     The GeeResponse data (unused).
     * @param cacheKey The cache key (unused).
     */
    @Override
    protected void saveToStorage(ProcessingResponse data, String cacheKey) {
        log.debug("saveToStorage called for GeeResponse (key: {}). No action taken as it's not persisted.", cacheKey);
        // GeeResponse objects are not stored persistently.
    }

    @PostConstruct
    public void scheduleCacheCleanup() {
        taskScheduler.schedule(this::cleanInfrequentlyUsedCache, new CronTrigger(cronExpression));
        log.info("Scheduled cache cleanup task with cron expression: {}", cronExpression);
    }

}
