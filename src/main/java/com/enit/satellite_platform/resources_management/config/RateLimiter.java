package com.enit.satellite_platform.resources_management.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {


    private final int maxRequests;
    private final long timeWindowMillis;
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> timestamps = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, long timeWindowMillis) {
        this.maxRequests = maxRequests;
        this.timeWindowMillis = timeWindowMillis;
    }

    public boolean isAllowed(String key) {
        long now = System.currentTimeMillis();

        timestamps.putIfAbsent(key, now);
        requestCounts.putIfAbsent(key, new AtomicInteger(0));

        long windowStart = timestamps.get(key);
        if (now - windowStart > timeWindowMillis) {
            // Reset the window
            timestamps.put(key, now);
            requestCounts.put(key, new AtomicInteger(0));
        }

        return requestCounts.get(key).incrementAndGet() <= maxRequests;
    }
}
