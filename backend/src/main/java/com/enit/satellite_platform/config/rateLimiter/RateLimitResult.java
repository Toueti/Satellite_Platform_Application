package com.enit.satellite_platform.config.rateLimiter;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@AllArgsConstructor
@Builder
public class RateLimitResult {
    
    /**
     * Whether the request is allowed under the rate limit
     */
    private final boolean allowed;
    
    /**
     * Number of remaining requests in the current time window
     */
    private final long remainingRequests;
    
    /**
     * Timestamp in milliseconds when the current time window will reset
     */
    private final long resetTimeMs;
    
    /**
     * Total number of requests allowed in the time window
     */
    private final long totalRequests;

    /**
     * Creates a result indicating the request was blocked
     */
    public static RateLimitResult blocked(long resetTimeMs, long totalRequests) {
        return new RateLimitResult(false, 0, resetTimeMs, totalRequests);
    }

    /**
     * Creates a result indicating the request was allowed
     */
    public static RateLimitResult allowed(long remainingRequests, long resetTimeMs, long totalRequests) {
        return new RateLimitResult(true, remainingRequests, resetTimeMs, totalRequests);
    }
} 