package com.enit.satellite_platform.config.rateLimiter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope; // Restored import
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;
@RefreshScope // Restored annotation
@Component
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitProperties {
    
    /**
     * Maximum number of requests allowed within the time window
     */
    @Value("${rate_limit.max_requests:10}")
    private int maxRequests;

    /**
     * Time window in milliseconds
     */
    @Value("${rate_limit.time_window_millis:60000}")
    private long timeWindowMillis;

    /**
     * Whether to enable rate limiting
     */
    private boolean enabled = true;

    /**
     * Name of the header to identify clients (optional)
     */
    private String clientIdHeader = "X-Client-ID";
}
