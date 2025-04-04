package com.enit.satellite_platform.config.cache_handler.general_cache_handler;

import lombok.Data;

@Data
public class CachePropertiesBase {

    private long redisTtlSeconds;

    private String cachePrefix;

    private int maxInfrequentAccessCount;

    private long inactivityThresholdDays;

    public CachePropertiesBase(long redisTtlSeconds, String cachePrefix, int maxInfrequentAccessCount, long inactivityThresholdDays) {
        this.redisTtlSeconds = redisTtlSeconds;
        this.cachePrefix = cachePrefix;
        this.maxInfrequentAccessCount = maxInfrequentAccessCount;
        this.inactivityThresholdDays = inactivityThresholdDays;
    }

}
