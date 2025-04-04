package com.enit.satellite_platform.config.redisConfig;
// package com.enit.satellite_platform.resources_management.config;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.stereotype.Component;

// import java.util.concurrent.TimeUnit;

// @Component
// public class RedisRateLimiter {

//     @Autowired
//     private RedisTemplate<String, String> redisTemplate;

//     public boolean isAllowed(String key, int maxRequests, long timeWindowMillis) {
//         String redisKey = "rate_limit:" + key;
//         long currentTime = System.currentTimeMillis();

//         // Remove expired requests
//         redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, currentTime - timeWindowMillis);

//         // Count requests in the current window
//         Long count = redisTemplate.opsForZSet().zCard(redisKey);
//         long requestCount = count != null ? count : 0L;

//         if (requestCount < maxRequests) {
//             // Add the current request to the set
//             redisTemplate.opsForZSet().add(redisKey, String.valueOf(currentTime), currentTime);
//             // Set TTL for auto-cleanup
//             redisTemplate.expire(redisKey, timeWindowMillis, TimeUnit.MILLISECONDS); 
//             return true;
//         }

//         return false;
//     }
// }
