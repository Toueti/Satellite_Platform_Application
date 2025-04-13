package com.enit.satellite_platform.modules.resource_management.utils.serialization;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Factory for creating and retrieving serializers.
 */
@Component
public class SerializerFactory {
    
    private final Map<SerializationFormat, Serializer> serializers = new HashMap<>();
    
    public SerializerFactory(JsonSerializer jsonSerializer) {
        serializers.put(SerializationFormat.JSON, jsonSerializer);
        // Add other serializers as they are implemented
    }
    
    /**
     * Get a serializer for the specified format
     * 
     * @param format The serialization format
     * @return The appropriate serializer
     */
    public Serializer getSerializer(SerializationFormat format) {
        Serializer serializer = serializers.get(format);
        if (serializer == null) {
            throw new IllegalArgumentException("No serializer found for format: " + format);
        }
        return serializer;
    }
    
    /**
     * Get a serializer by content type (e.g., "application/json")
     * 
     * @param contentType The content type
     * @return The appropriate serializer
     */
    public Serializer getSerializerByContentType(String contentType) {
        if (contentType == null) {
            return getSerializer(SerializationFormat.JSON); // Default
        }
        
        String normalizedContentType = contentType.toLowerCase();
        
        if (normalizedContentType.contains("json")) {
            return getSerializer(SerializationFormat.JSON);
        }
        // Add other content type mappings as needed
        
        // Default to JSON if no match
        return getSerializer(SerializationFormat.JSON);
    }
}
