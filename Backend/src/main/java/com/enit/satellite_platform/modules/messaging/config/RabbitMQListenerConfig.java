package com.enit.satellite_platform.modules.messaging.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.cloud.context.config.annotation.RefreshScope; // Import RefreshScope
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@RequiredArgsConstructor
@Slf4j
@RefreshScope // Add RefreshScope annotation
public class RabbitMQListenerConfig {

    private final Environment environment;
    private final ConnectionFactory connectionFactory; // Autowired by Spring Boot AMQP starter
    private final MessageConverter messageConverter; // Autowired from RabbitMQConfig

    // Config key for prefetch count
    private static final String PREFETCH_COUNT_PROPERTY = "messaging.queue.prefetch.count";
    // Default prefetch count
    private static final int DEFAULT_PREFETCH_COUNT = 10;

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        // Set prefetch count from configuration
        int prefetchCount = getPrefetchCount();
        factory.setPrefetchCount(prefetchCount);
        log.info("Setting RabbitMQ listener prefetch count to: {}", prefetchCount);

        // Configure other factory settings if needed (e.g., acknowledge mode, concurrency)
        // factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // Example if manual ack is needed

        return factory;
    }

    private int getPrefetchCount() {
        String prefetchStr = environment.getProperty(PREFETCH_COUNT_PROPERTY, String.valueOf(DEFAULT_PREFETCH_COUNT));
        try {
            int prefetch = Integer.parseInt(prefetchStr);
            if (prefetch <= 0) {
                 log.warn("Invalid non-positive value '{}' for property '{}'. Using default {}.", prefetch, PREFETCH_COUNT_PROPERTY, DEFAULT_PREFETCH_COUNT);
                 return DEFAULT_PREFETCH_COUNT;
            }
            return prefetch;
        } catch (NumberFormatException e) {
            log.error("Invalid format for property '{}': '{}'. Using default value {}.", PREFETCH_COUNT_PROPERTY, prefetchStr, DEFAULT_PREFETCH_COUNT, e);
            return DEFAULT_PREFETCH_COUNT;
        }
    }
}
