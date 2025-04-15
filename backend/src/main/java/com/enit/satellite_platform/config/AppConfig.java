package com.enit.satellite_platform.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableCaching
@EnableRetry
// @EnableAsync // Already enabled in SatellitePlatformApplication
public class AppConfig {

    /**
     * Provides the primary TaskExecutor bean for running asynchronous tasks
     * annotated with @Async. This executor is configured with a thread pool
     * suitable for general background tasks.
     *
     * @return The primary TaskExecutor instance.
     */
    @Bean(name = "taskExecutor") // Explicitly name the bean
    @Primary // Mark this as the primary TaskExecutor
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Set core pool size
        executor.setMaxPoolSize(10); // Set max pool size
        executor.setQueueCapacity(25); // Set queue capacity
        executor.setThreadNamePrefix("AsyncTask-");
        executor.initialize();
        return executor;
    }
}
