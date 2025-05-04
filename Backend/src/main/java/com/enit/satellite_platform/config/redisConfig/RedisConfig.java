package com.enit.satellite_platform.config.redisConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Configuration
public class RedisConfig {

    /**
     * Provides a generic RedisTemplate bean configured for storing various objects
     * using JSON serialization with type information.
     *
     * @param connectionFactory The RedisConnectionFactory.
     * @return A generic RedisTemplate<String, Object> instance.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure ObjectMapper for JSON serialization with type info
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Handle Java 8 date/time types
        // Enable default typing to store class information in JSON.
        // Use NON_FINAL for security and flexibility. Adjust validator as needed.
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(), // Use default validator
            ObjectMapper.DefaultTyping.NON_FINAL,       // Store type info for non-final types
            JsonTypeInfo.As.PROPERTY                    // Store type info as a property (e.g., "@class")
        );

        // Create the Jackson serializer
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // Configure the template serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
