package com.enit.satellite_platform.config.rateLimiter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimitData {
        private final AtomicLong windowStart;
        private final AtomicInteger count;

        public RateLimitData(long windowStart) {
            this.windowStart = new AtomicLong(windowStart);
            this.count = new AtomicInteger(0);
        }

        public long getWindowStart() {
            return windowStart.get();
        }

        public void reset(long newWindowStart) {
            windowStart.set(newWindowStart);
            count.set(0);
        }

        public int incrementCount() {
            return count.incrementAndGet();
        }
    }