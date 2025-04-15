package com.enit.satellite_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;
import org.springframework.messaging.simp.config.MessageBrokerRegistry; // Add WebSocket imports
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker; // Add WebSocket imports
import org.springframework.web.socket.config.annotation.StompEndpointRegistry; // Add WebSocket imports
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer; // Add WebSocket imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean; // Add Bean import
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
// Re-add imports for explicit scheduler configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.annotation.SchedulingConfigurer;


// import com.enit.satellite_platform.modules.monitoring.config.WebSocketConfig; // Remove WebSocketConfig import

/**
 * The main entry point for the Satellite Platform application. This class configures and runs the Spring Boot
 * application. It uses Spring Boot's auto-configuration feature but excludes the DataSourceAutoConfiguration
 * since the application uses MongoDB. Auditing and asynchronous method execution are also enabled.
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableConfigurationProperties(com.enit.satellite_platform.config.cache_handler.CacheProperties.class)
@EnableMongoAuditing
@EnableAsync
@EnableScheduling
@EnableMongoRepositories(basePackages = "com.enit.satellite_platform")
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
// @Import(WebSocketConfig.class) // Remove explicit import
@EnableWebSocketMessageBroker // Add annotation here
public class SatellitePlatformApplication implements WebSocketMessageBrokerConfigurer, SchedulingConfigurer { // Implement SchedulingConfigurer again

    private static final Logger log = LoggerFactory.getLogger(SatellitePlatformApplication.class);

    /**
     * The main method to run the Satellite Platform application.
     *
     * @param args Command line arguments passed to the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(SatellitePlatformApplication.class, args);
    }

    // --- WebSocket Configuration Methods ---

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("Registering STOMP endpoint directly in main app: /monitoring-websocket with SockJS");
        registry.addEndpoint("/monitoring-websocket")
                .setAllowedOrigins("*")
                 .withSockJS();
    }

    // --- Explicit Task Scheduler Configuration Re-added ---

    @Bean(destroyMethod="shutdown") // Ensure scheduler shuts down gracefully
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5); // Adjust pool size as needed
        scheduler.setThreadNamePrefix("log-queue-scheduler-"); // Specific prefix
        scheduler.setErrorHandler(t -> log.error("Error occurred in scheduled task", t)); // Add error handler
        scheduler.initialize();
        log.info("!!! Explicit ThreadPoolTaskScheduler created !!!"); // Add log
        return scheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        log.info("!!! Configuring tasks with explicit scheduler !!!"); // Add log
        // Explicitly set the scheduler for @Scheduled tasks
        taskRegistrar.setTaskScheduler(taskScheduler());
    }
}
