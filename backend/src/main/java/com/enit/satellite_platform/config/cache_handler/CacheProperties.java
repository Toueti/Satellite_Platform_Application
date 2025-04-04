package com.enit.satellite_platform.config.cache_handler;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import com.enit.satellite_platform.config.cache_handler.general_cache_handler.CachePropertiesBase;

import org.springframework.beans.factory.annotation.Value;


@Component
@RefreshScope
@ConfigurationProperties(prefix = "cache")
public class CacheProperties extends CachePropertiesBase {

        // Constructor for CacheProperties
        public CacheProperties(
                @Value("${cache.redis.ttl_seconds:604800}") long redisTtlSeconds,
                @Value("${cache.redis.prefix:cache:data:}") String cachePrefix,
                @Value("${cache.cleanup.max_infrequent_access_count:3}") int maxInfrequentAccessCount,
                @Value("${cache.cleanup.inactivity_threshold_days:2}") long inactivityThresholdDays) {
                super(redisTtlSeconds, cachePrefix, maxInfrequentAccessCount, inactivityThresholdDays);
        }
    
}
