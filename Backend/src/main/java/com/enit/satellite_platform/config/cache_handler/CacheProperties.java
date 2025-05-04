package com.enit.satellite_platform.config.cache_handler;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import com.enit.satellite_platform.config.cache_handler.general_cache_handler.CachePropertiesBase;

/**
 * Concrete implementation holding cache configuration properties, loaded via Spring Boot's
 * {@code @ConfigurationProperties} mechanism.
 *
 * This class binds properties defined under the "cache.redis" prefix in application configuration files
 * (e.g., application.properties, application.yml) to the fields inherited from {@link CachePropertiesBase}.
 *
 * The {@code @RefreshScope} annotation allows these properties to be refreshed dynamically
 * (e.g., via Spring Cloud Config) without restarting the application.
 *
 * @see CachePropertiesBase
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 * @see org.springframework.cloud.context.config.annotation.RefreshScope
 */
@RefreshScope
@ConfigurationProperties(prefix = "cache.redis")
public class CacheProperties extends CachePropertiesBase {

    /**
     * Default constructor with values from application.properties
     */
    public CacheProperties() {
        super(
            604800, // Default TTL from application.properties (cache.redis.ttl_seconds)
            "cache:data:", // Default prefix from application.properties (cache.redis.prefix)
            3, // Default max infrequent access count
            2 // Default inactivity threshold days
        );
    }
}
