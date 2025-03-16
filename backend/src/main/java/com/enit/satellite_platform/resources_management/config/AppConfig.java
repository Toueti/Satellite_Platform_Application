package com.enit.satellite_platform.resources_management.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.retry.annotation.EnableRetry;

import com.enit.satellite_platform.resources_management.dto.GeeRequest;
import com.enit.satellite_platform.resources_management.dto.GeeResponse;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableCaching
@EnableRetry
public class AppConfig {


    /**
     * Provides a RedisTemplate bean for interacting with Redis.
     *
     * @param connectionFactory The RedisConnectionFactory.
     * @return The RedisTemplate instance.
     */
    @Bean
    public RedisTemplate<String, GeeResponse> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, GeeResponse> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(GeeResponse.class));
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Provides a RedisTemplate bean for interacting with Redis to store GeeRequest objects.
     *
     * @param connectionFactory The RedisConnectionFactory.
     * @return The RedisTemplate instance.
     */
    @Bean
    public RedisTemplate<String, GeeRequest> redisRequestTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, GeeRequest> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(GeeRequest.class));
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Provides an Executor bean for running asynchronous tasks. This executor
     * provides a fixed thread pool with 10 threads. The pool is used to execute
     * asynchronous tasks such as retrying failed GEE requests.
     * 
     * @return The Executor instance.
     */
    @Bean
    public Executor asyncTaskExecutor() {
        return Executors.newFixedThreadPool(10);
    }
}
