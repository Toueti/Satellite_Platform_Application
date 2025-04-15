package com.enit.satellite_platform.modules.resource_management.utils.serialization;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON implementation of the Serializer interface using Jackson.
 */
@Component
public class JsonSerializer implements Serializer {
    private static final Logger logger = LoggerFactory.getLogger(JsonSerializer.class);
    private final ObjectMapper objectMapper;
    
    public JsonSerializer() {
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize object to JSON", e);
            throw new SerializationException("Failed to serialize object to JSON", e);
        }
    }
    
    @Override
    public <T> T deserialize(String data, Class<T> clazz) {
        try {
            return objectMapper.readValue(data, clazz);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize JSON to object of type {}", clazz.getName(), e);
            throw new SerializationException("Failed to deserialize JSON", e);
        }
    }
    
    @Override
    public Map<String, Object> deserializeToMap(String data) {
        try {
            return objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize JSON to map", e);
            throw new SerializationException("Failed to deserialize JSON to map", e);
        }
    }
    
    @Override
    public String getContentType() {
        return "application/json";
    }
}
