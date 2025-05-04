package com.enit.satellite_platform.config.rateLimiter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RefreshScope
public class RateLimiter {
    private final int maxRequests;
    private final long timeWindowMillis;
    private final Map<String, RateLimitData> rateLimitData = new ConcurrentHashMap<>();
    private final long cleanupThreshold;

    public RateLimiter(@Value("${rate_limit.max_requests}") int maxRequests,
                       @Value("${rate_limit.time_window_millis}") long timeWindowMillis) {
        this.maxRequests = maxRequests;
        this.timeWindowMillis = timeWindowMillis;
        this.cleanupThreshold = timeWindowMillis * 2; // Clean up entries older than 2 time windows
    }

    public RateLimitResult checkLimit(String key) {
        try {
            long now = Instant.now().toEpochMilli();
            RateLimitData data = rateLimitData.computeIfAbsent(key, k -> new RateLimitData(now));

            synchronized (data) {
                if (now - data.getWindowStart() > timeWindowMillis) {
                    // Reset the window
                    data.reset(now);
                }

                int currentCount = data.incrementCount();
                long resetTime = data.getWindowStart() + timeWindowMillis;

                if (currentCount <= maxRequests) {
                    return RateLimitResult.allowed(
                        maxRequests - currentCount,
                        resetTime,
                        maxRequests
                    );
                } else {
                    return RateLimitResult.blocked(
                        resetTime,
                        maxRequests
                    );
                }
            }
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            return RateLimitResult.allowed(maxRequests, Instant.now().toEpochMilli() + timeWindowMillis, maxRequests);
        }
    }

    @Scheduled(fixedRateString = "${rate-limit.cleanup-interval:300000}") // Default: 5 minutes
    public void cleanupOldEntries() {
        try {
            long now = Instant.now().toEpochMilli();
            rateLimitData.entrySet().removeIf(entry -> 
                now - entry.getValue().getWindowStart() > cleanupThreshold
            );
        } catch (Exception e) {
            log.error("Error cleaning up old rate limit entries", e);
        }
    }
}
