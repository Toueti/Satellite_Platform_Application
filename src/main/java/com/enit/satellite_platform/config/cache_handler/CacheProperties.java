package com.enit.satellite_platform.config.cache_handler;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
// import org.springframework.stereotype.Component; // Removed

import com.enit.satellite_platform.config.cache_handler.general_cache_handler.CachePropertiesBase;

/**
 * Concrete implementation holding cache configuration properties, loaded via Spring Boot's
 * {@code @ConfigurationProperties} mechanism.
 *
 * This class binds properties defined under the "cache" prefix in application configuration files
 * (e.g., application.properties, application.yml) to the fields inherited from {@link CachePropertiesBase}.
 *
 * The {@code @Component} annotation makes it a Spring-managed bean, available for injection.
 * The {@code @RefreshScope} annotation allows these properties to be refreshed dynamically
 * (e.g., via Spring Cloud Config) without restarting the application.
 *
 * @see CachePropertiesBase
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 * @see org.springframework.cloud.context.config.annotation.RefreshScope
 */
// @Component // Removed - Bean creation handled by @EnableConfigurationProperties
@RefreshScope
@ConfigurationProperties(prefix = "cache")
public class CacheProperties extends CachePropertiesBase {

    /**
     * Constructor used by Spring Boot for {@code @ConfigurationProperties} binding.
     * Spring automatically injects the values from the configuration source (e.g., application.properties)
     * matching the parameter names (converted to kebab-case like 'redis-ttl-seconds') into this constructor.
     * These values are then passed to the superclass constructor.
     *
     * @param redisTtlSeconds          Bound from {@code cache.redis-ttl-seconds}. See {@link CachePropertiesBase#redisTtlSeconds}.
     * @param cachePrefix              Bound from {@code cache.cache-prefix}. See {@link CachePropertiesBase#cachePrefix}.
     * @param maxInfrequentAccessCount Bound from {@code cache.max-infrequent-access-count}. See {@link CachePropertiesBase#maxInfrequentAccessCount}.
     * @param inactivityThresholdDays  Bound from {@code cache.inactivity-threshold-days}. See {@link CachePropertiesBase#inactivityThresholdDays}.
     */
    public CacheProperties(
            long redisTtlSeconds,
            String cachePrefix,
            int maxInfrequentAccessCount,
            long inactivityThresholdDays) {
        super(redisTtlSeconds, cachePrefix, maxInfrequentAccessCount, inactivityThresholdDays);
        // Values are injected directly into constructor parameters by Spring Boot
        // and passed to the superclass constructor.
    }
}
