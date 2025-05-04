package com.enit.satellite_platform.modules.resource_management.utils.serialization;

import java.util.Map;

/**
 * Interface for serializing and deserializing objects to/from various formats.
 */
public interface Serializer {
    /**
     * Serialize an object to a string representation
     * 
     * @param obj The object to serialize
     * @return The serialized string
     */
    String serialize(Object obj);
    
    /**
     * Deserialize a string to an object of the specified class
     * 
     * @param <T> The type to deserialize to
     * @param data The string data to deserialize
     * @param clazz The class to deserialize to
     * @return The deserialized object
     */
    <T> T deserialize(String data, Class<T> clazz);
    
    /**
     * Deserialize a string to a Map
     * 
     * @param data The string data to deserialize
     * @return The deserialized map
     */
    Map<String, Object> deserializeToMap(String data);
    
    /**
     * Get the content type of this serializer (e.g., application/json)
     * 
     * @return The content type string
     */
    String getContentType();
}
