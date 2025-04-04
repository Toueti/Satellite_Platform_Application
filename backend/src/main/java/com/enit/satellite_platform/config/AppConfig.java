package com.enit.satellite_platform.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableCaching
@EnableRetry
public class AppConfig {

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
