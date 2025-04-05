package com.enit.satellite_platform.config.cache_handler.general_cache_handler;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

/**
 * The generated key is set on the entity before it is saved to the database.
 * <p>
 * This class is a Spring component, so it will be automatically detected and registered by Spring's component scanning.
 * </p>
 * It interacts with the {@link ICacheKeyGenerator } interface to generate a cache key for the entity.
 */
@Component
public class CacheKeyGenerationListener extends AbstractMongoEventListener<Object> {

    private final ICacheKeyGenerator cacheKeyGenerator;
    
    public CacheKeyGenerationListener(ICacheKeyGenerator cacheKeyGenerator) {
        this.cacheKeyGenerator = cacheKeyGenerator;
    }

    @Override
    public void onBeforeSave(BeforeSaveEvent<Object> event) {
        Object entity = event.getSource();
        if (entity instanceof CacheableEntity) {
            CacheableEntity cacheableEntity = (CacheableEntity) entity;
            if (cacheableEntity.getCacheKey() == null) { // Only set if not already set
                String generatedKey = cacheKeyGenerator.generateKey(cacheableEntity);
                cacheableEntity.setCacheKey(generatedKey);
            }
        }
    }
}


